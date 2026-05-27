package com.example.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AfriRentViewModel(
    private val repository: AfriRentRepository
) : ViewModel() {

    // Language State: "EN" or "HA"
    private val _currentLanguage = MutableStateFlow("EN")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "EN") "HA" else "EN"
    }

    fun translate(key: String, vararg formatArgs: Any): String {
        val base = Localization.translate(key, _currentLanguage.value)
        return if (formatArgs.isNotEmpty()) {
            try {
                String.format(base, *formatArgs)
            } catch (e: Exception) {
                base
            }
        } else {
            base
        }
    }

    // Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Navigation state: "auth", "home", "property_detail", "chat", "dashboard", "admin", "add_property"
    private val _currentScreen = MutableStateFlow("auth")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Selected state for details & requests
    private val _selectedPropertyId = MutableStateFlow<Long?>(null)
    val selectedPropertyId: StateFlow<Long?> = _selectedPropertyId.asStateFlow()

    private val _selectedRequestId = MutableStateFlow<Long?>(null)
    val selectedRequestId: StateFlow<Long?> = _selectedRequestId.asStateFlow()

    // Filters
    val searchQuery = MutableStateFlow("")
    val filterPriceMax = MutableStateFlow(1000000.0)
    val filterRooms = MutableStateFlow(0) // 0 means any
    val filterPropertyType = MutableStateFlow("Any") // "Any", "Apartment", "House", "Bungalow", "Self-Contain"
    val filterState = MutableStateFlow("Borno State")
    val filterLga = MutableStateFlow("Gwoza")

    // Database flows
    val allProperties: StateFlow<List<Property>> = repository.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceipts: StateFlow<List<PaymentReceipt>> = repository.getAllReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Properties Output
    val filteredProperties: StateFlow<List<Property>> = combine(
        allProperties,
        searchQuery,
        filterPriceMax,
        filterRooms
    ) { props, query, maxPrice, rms ->
        props.filter { p ->
            val matchQuery = query.isEmpty() || 
                p.title.contains(query, ignoreCase = true) || 
                p.description.contains(query, ignoreCase = true) ||
                p.address.contains(query, ignoreCase = true)
            
            val matchPrice = p.pricePerYear <= maxPrice
            val matchRooms = rms == 0 || p.rooms == rms
            matchQuery && matchPrice && matchRooms
        }
    }.combine(
        combine(filterPropertyType, filterState, filterLga) { type, st, lg -> Triple(type, st, lg) }
    ) { filteredList, triple ->
        val (type, st, lg) = triple
        filteredList.filter { p ->
            val matchType = type == "Any" || p.propertyType.equals(type, ignoreCase = true)
            val matchState = st.isEmpty() || p.state.equals(st, ignoreCase = true)
            val matchLga = lg.isEmpty() || p.lga.equals(lg, ignoreCase = true)
            matchType && matchState && matchLga
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Property details
    val selectedProperty: StateFlow<Property?> = _selectedPropertyId
        .map { id -> if (id != null) repository.getProperty(id) else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reviews for active property
    val selectedPropertyReviews: StateFlow<List<Review>> = _selectedPropertyId
        .flatMapLatest { id ->
            if (id != null) repository.getReviewsForProperty(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat Messages for current focused property/lease
    val chatMessages: StateFlow<List<ChatMessage>> = _selectedPropertyId
        .flatMapLatest { propId ->
            if (propId != null) repository.getChatMessages(propId) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active User requests (Dashboard)
    val userRentalRequests: StateFlow<List<RentalRequest>> = _currentUser
        .flatMapLatest { usr ->
            if (usr == null) flowOf(emptyList())
            else if (usr.role == "Tenant") repository.getRequestsForTenant(usr.id)
            else repository.getRequestsForLandlord(usr.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Rent Payment Reminders list for Tenants
    val activeReminders: StateFlow<List<String>> = combine(userRentalRequests, allProperties) { requests, props ->
        val list = mutableListOf<String>()
        requests.filter { it.status == "Accepted" && !it.agreementSigned }.forEach { req ->
            val prop = props.find { p -> p.id == req.propertyId }
            if (prop != null) {
                list.add("Lease Approved for ${prop.title}! 7 days left to sign digital agreement.")
                list.add("Payment Reminder (3 days before rent commencement): ₦${req.durationYears * prop.pricePerYear} due on commencement.")
            }
        }
        requests.filter { it.status == "Paid" }.forEach { req ->
            val prop = props.find { p -> p.id == req.propertyId }
            if (prop != null) {
                list.add("Active Tenancy for ${prop.title} is secure. Next renewal reminder will be issued annually.")
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Screen navigation helpers
    fun navigateTo(screen: String, propertyId: Long? = null, requestId: Long? = null) {
        _currentScreen.value = screen
        if (propertyId != null) {
            _selectedPropertyId.value = propertyId
        }
        if (requestId != null) {
            _selectedRequestId.value = requestId
        }
    }

    fun goBack() {
        val current = _currentScreen.value
        _currentScreen.value = when (current) {
            "property_detail" -> "home"
            "chat" -> "property_detail"
            "add_property" -> "home"
            "dashboard" -> "home"
            "admin" -> "home"
            else -> "auth"
        }
    }

    // Auth actions
    fun authenticate(emailOrPhone: String, role: String, fullName: String) {
        viewModelScope.launch {
            val trimmed = emailOrPhone.trim()
            var existingUser = repository.getUser(trimmed)
            if (existingUser == null) {
                existingUser = User(
                    id = trimmed,
                    phone = if (trimmed.contains("@")) "08030000000" else trimmed,
                    email = if (trimmed.contains("@")) trimmed else "$trimmed@afrirent.com",
                    fullName = fullName.ifEmpty { "User Gwoza" },
                    role = role,
                    avatarUrl = "avatar_default",
                    isVerified = false,
                    idNumber = ""
                )
                repository.saveUser(existingUser)
            } else {
                // If switching role during demo Login
                if (role != existingUser.role) {
                    existingUser = existingUser.copy(role = role)
                    repository.saveUser(existingUser)
                }
            }
            _currentUser.value = existingUser
            if (existingUser.role == "Admin") {
                _currentScreen.value = "admin"
            } else {
                _currentScreen.value = "home"
            }
        }
    }

    fun verifyIdentity(ninOrBvn: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val updated = user.copy(
                isVerified = true,
                idNumber = ninOrBvn
            )
            repository.saveUser(updated)
            _currentUser.value = updated
        }
    }

    fun unverifyCurrentUser() {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val updated = user.copy(
                isVerified = false,
                idNumber = ""
            )
            repository.saveUser(updated)
            _currentUser.value = updated
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = "auth"
    }

    // Property action
    fun publishProperty(
        title: String,
        description: String,
        priceStr: String,
        address: String,
        rooms: Int,
        type: String,
        amenities: String,
        state: String,
        lga: String,
        city: String
    ) {
        val user = _currentUser.value ?: return
        val price = priceStr.toDoubleOrNull() ?: 100000.0

        viewModelScope.launch {
            val newProperty = Property(
                landlordId = user.id,
                title = title,
                description = description,
                pricePerYear = price,
                address = address,
                country = "Nigeria",
                state = state,
                lga = lga,
                city = city,
                street = address,
                rooms = rooms,
                propertyType = type,
                amenities = amenities,
                imageUrl = "property_apartment_1",
                latitude = 11.0837 + (Math.random() - 0.5) * 0.02,
                longitude = 13.6934 + (Math.random() - 0.5) * 0.02,
                status = "Available"
            )
            repository.saveProperty(newProperty)
            _currentScreen.value = "home"
        }
    }

    fun deleteProperty(property: Property) {
        viewModelScope.launch {
            repository.deleteProperty(property)
        }
    }

    // Lease requests and agreements
    fun applyForLease(durationYears: Int, paymentInterval: String) {
        val user = _currentUser.value ?: return
        val prop = selectedProperty.value ?: return

        viewModelScope.launch {
            val agreementTemplate = """
                AFRIRENT DIGITAL TENANCY CONTRACT
                
                This Contract of Long-Term Residential Lease is made and digitally enacted this day between:
                LANDLORD: ${repository.getUser(prop.landlordId)?.fullName ?: "Owner"}
                TENANT: ${user.fullName}
                
                PROPERTY DESCRIPTION:
                ${prop.title} located at ${prop.address}, ${prop.city}, ${prop.lga}, ${prop.state}, Nigeria.
                
                LEASE INSTRUCTIONS & DURATION:
                1. The lease duration is strictly fixed at $durationYears year(s).
                2. Payment schedule is agreed on a $paymentInterval basis.
                3. The total annual cost is ₦${prop.pricePerYear}. Total due value is ₦${prop.pricePerYear * durationYears}.
                4. Sub-letting of Gwoza properties without written permission is strictly forbidden.
                
                This lease is backed by Gwoza Local Administration safety policies.
            """.trimIndent()

            val request = RentalRequest(
                propertyId = prop.id,
                tenantId = user.id,
                landlordId = prop.landlordId,
                durationYears = durationYears,
                paymentInterval = paymentInterval,
                status = "Pending",
                agreementSigned = false,
                agreementText = agreementTemplate
            )
            repository.saveRentalRequest(request)
            _currentScreen.value = "dashboard"
        }
    }

    fun respondToRentalRequest(requestId: Long, isAccepted: Boolean) {
        viewModelScope.launch {
            val req = repository.getRequestById(requestId) ?: return@launch
            val updated = req.copy(
                status = if (isAccepted) "Accepted" else "Rejected"
            )
            repository.updateRentalRequest(updated)
        }
    }

    fun signDigitalAgreement(requestId: Long) {
        viewModelScope.launch {
            val req = repository.getRequestById(requestId) ?: return@launch
            val updated = req.copy(
                agreementSigned = true
            )
            repository.updateRentalRequest(updated)
        }
    }

    // simulated Payment Completion (integrates Paystack & Flutterwave checkouts)
    fun processSimulatedPayment(requestId: Long, gateway: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val req = repository.getRequestById(requestId) ?: return@launch
            val property = repository.getProperty(req.propertyId) ?: return@launch

            val isAnnual = req.paymentInterval == "Annually"
            val totalBill = if (isAnnual) property.pricePerYear * req.durationYears else (property.pricePerYear / 12)

            // 1. Mark request as paid & property status as rented
            val updatedReq = req.copy(status = "Paid")
            repository.updateRentalRequest(updatedReq)

            val updatedProp = property.copy(status = "Rented")
            repository.updateProperty(updatedProp)

            // 2. Insert payment receipt in Room DB
            val receiptNo = "AR-${gateway.substring(0,3).uppercase()}-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
            val newReceipt = PaymentReceipt(
                rentalRequestId = requestId,
                propertyId = property.id,
                tenantId = user.id,
                amount = totalBill,
                paymentGateway = gateway,
                receiptNumber = receiptNo
            )
            repository.saveReceipt(newReceipt)

            // Add automated review trigger
            repository.saveReview(
                 Review(
                     propertyId = property.id,
                     reviewerId = "system@afrirent.com",
                     revieweeId = property.landlordId,
                     rating = 5.0f,
                     comment = "Paid securely via $gateway. Receipt $receiptNo is archived offline."
                 )
            )
        }
    }

    // In-app messaging
    fun sendChatMessage(msgText: String) {
        val user = _currentUser.value ?: return
        val propId = _selectedPropertyId.value ?: return
        val prop = selectedProperty.value ?: return
        if (msgText.trim().isEmpty()) return

        val recipientId = if (user.id == prop.landlordId) {
            // Landlord is messaging. We target the active request tenant
            // Find request
            val currentReq = userRentalRequests.value.firstOrNull { it.propertyId == propId }
            currentReq?.tenantId ?: "tenant@afrirent.com"
        } else {
            prop.landlordId
        }

        viewModelScope.launch {
            val chat = ChatMessage(
                propertyId = propId,
                senderId = user.id,
                receiverId = recipientId,
                messageText = msgText
            )
            repository.saveChatMessage(chat)
        }
    }

    // Review submitting
    fun submitReview(rating: Float, comment: String) {
        val user = _currentUser.value ?: return
        val propId = _selectedPropertyId.value ?: return
        if (comment.trim().isEmpty()) return

        viewModelScope.launch {
            val review = Review(
                propertyId = propId,
                reviewerId = user.id,
                revieweeId = selectedProperty.value?.landlordId ?: "landlord@afrirent.com",
                rating = rating,
                comment = comment
            )
            repository.saveReview(review)
        }
    }

    // Admin dashboard specific approval
    fun approveUserIdentityAndRefresh(userId: String) {
        viewModelScope.launch {
            val u = repository.getUser(userId)
            if (u != null) {
                val updated = u.copy(isVerified = true, idNumber = "NIN-29381048572")
                repository.saveUser(updated)
            }
        }
    }
}
