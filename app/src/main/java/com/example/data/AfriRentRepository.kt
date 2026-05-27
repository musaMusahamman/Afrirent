package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AfriRentRepository(
    private val db: AppDatabase
) {
    val userDao = db.userDao()
    val propertyDao = db.propertyDao()
    val rentalRequestDao = db.rentalRequestDao()
    val chatDao = db.chatDao()
    val paymentDao = db.paymentDao()
    val reviewDao = db.reviewDao()

    // Users
    suspend fun getUser(id: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(id)
    }

    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    // Properties
    fun getAllProperties(): Flow<List<Property>> = propertyDao.getAllProperties()
    
    suspend fun getProperty(id: Long): Property? = withContext(Dispatchers.IO) {
        propertyDao.getPropertyById(id)
    }

    suspend fun saveProperty(property: Property) = withContext(Dispatchers.IO) {
        propertyDao.insertProperty(property)
    }

    suspend fun updateProperty(property: Property) = withContext(Dispatchers.IO) {
        propertyDao.updateProperty(property)
    }

    suspend fun deleteProperty(property: Property) = withContext(Dispatchers.IO) {
        propertyDao.deleteProperty(property)
    }

    // Rental Requests
    fun getAllRentalRequests(): Flow<List<RentalRequest>> = rentalRequestDao.getAllRentalRequests()

    fun getRequestsForTenant(tenantId: String): Flow<List<RentalRequest>> =
        rentalRequestDao.getRentalRequestsByTenant(tenantId)

    fun getRequestsForLandlord(landlordId: String): Flow<List<RentalRequest>> =
        rentalRequestDao.getRentalRequestsByLandlord(landlordId)

    suspend fun getRequestById(id: Long): RentalRequest? = withContext(Dispatchers.IO) {
        rentalRequestDao.getRequestById(id)
    }

    suspend fun saveRentalRequest(request: RentalRequest): Long = withContext(Dispatchers.IO) {
        rentalRequestDao.insertRequest(request)
    }

    suspend fun updateRentalRequest(request: RentalRequest) = withContext(Dispatchers.IO) {
        rentalRequestDao.updateRequest(request)
    }

    // Chat
    fun getChatMessages(propertyId: Long): Flow<List<ChatMessage>> =
        chatDao.getChatMessagesForProperty(propertyId)

    suspend fun saveChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    // Receipts
    fun getReceiptsForTenant(tenantId: String): Flow<List<PaymentReceipt>> =
        paymentDao.getReceiptsByTenant(tenantId)

    fun getAllReceipts(): Flow<List<PaymentReceipt>> = paymentDao.getAllReceipts()

    suspend fun saveReceipt(receipt: PaymentReceipt) = withContext(Dispatchers.IO) {
        paymentDao.insertReceipt(receipt)
    }

    // Reviews
    fun getReviewsForProperty(propertyId: Long): Flow<List<Review>> =
        reviewDao.getReviewsForProperty(propertyId)

    suspend fun saveReview(review: Review) = withContext(Dispatchers.IO) {
        reviewDao.insertReview(review)
    }

    // Database Seeding for First Launch
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        // We'll check if we have properties. If not, seed users and properties
        val usersFlow = userDao.getUserById("landlord@afrirent.com")
        if (usersFlow == null) {
            // Store seed users
            val defaultLandlord = User(
                id = "landlord@afrirent.com",
                phone = "08031234567",
                email = "landlord@afrirent.com",
                fullName = "Malam Ibrahim Gwoza",
                role = "Landlord",
                avatarUrl = "avatar_ibrahim",
                isVerified = true,
                idNumber = "NIN-29381048572",
                rating = 4.8f
            )
            val defaultTenant = User(
                id = "tenant@afrirent.com",
                phone = "08169876543",
                email = "tenant@afrirent.com",
                fullName = "Grace Bitrus",
                role = "Tenant",
                avatarUrl = "avatar_grace",
                isVerified = true,
                idNumber = "BVN-22245678901",
                rating = 4.6f
            )
            val defaultAdmin = User(
                id = "admin@afrirent.com",
                phone = "09011122233",
                email = "admin@afrirent.com",
                fullName = "Amina Musa",
                role = "Admin",
                avatarUrl = "avatar_amina",
                isVerified = true,
                idNumber = "NIN-98765432109",
                rating = 5.0f
            )
            userDao.insertUser(defaultLandlord)
            userDao.insertUser(defaultTenant)
            userDao.insertUser(defaultAdmin)

            // Store seed properties
            val p1 = Property(
                landlordId = "landlord@afrirent.com",
                title = "Gwoza Town Center Apartment",
                description = "A standard 2-bedroom flat with running water, reliable power, and a secured compound. Just minutes away from Gwoza local markets.",
                pricePerYear = 350000.0,
                address = "Opposite General Hospital, Bama Road",
                country = "Nigeria",
                state = "Borno State",
                lga = "Gwoza",
                city = "Gwoza Town",
                street = "Bama Road",
                rooms = 2,
                propertyType = "Apartment",
                amenities = "Water Running, Secured Fencing, Solar Power Backup",
                imageUrl = "property_apartment_1",
                latitude = 11.0837,
                longitude = 13.6944,
                status = "Available"
            )

            val p2 = Property(
                landlordId = "landlord@afrirent.com",
                title = "Gwoza Hills View Bungalow",
                description = "Modern 3-bedroom bungalow with a spacious veranda overviewing the iconic Gwoza hills. Peaceful, secure, and has a dedicated water borehole.",
                pricePerYear = 480000.0,
                address = "Near Local Government Secretariat, Hills View Area",
                country = "Nigeria",
                state = "Borno State",
                lga = "Gwoza",
                city = "Gwoza Town",
                street = "Secretariat Link",
                rooms = 3,
                propertyType = "Bungalow",
                amenities = "Borehole Water, Veranda, Compound Parking",
                imageUrl = "property_bungalow_2",
                latitude = 11.0797,
                longitude = 13.6894,
                status = "Available"
            )

            val p3 = Property(
                landlordId = "landlord@afrirent.com",
                title = "Furnished 1-Room Self-Contain",
                description = "Highly affordable single room self-contain with modern bathroom and small kitchenette. Ideal for single professionals, teachers, or humanitarian workers.",
                pricePerYear = 150000.0,
                address = "State Lowcost Housing, Block D",
                country = "Nigeria",
                state = "Borno State",
                lga = "Gwoza",
                city = "Gwoza Town",
                street = "Lowcost Lane",
                rooms = 1,
                propertyType = "Self-Contain",
                amenities = "Kitchenette, Tiled Floors, Close to Transport",
                imageUrl = "property_selfcontain_3",
                latitude = 11.0897,
                longitude = 13.6994,
                status = "Available"
            )

            val p4 = Property(
                landlordId = "unverified@afrirent.com", // Created by a second Landlord to show Pending approval
                title = "Polished Maiduguri Suburban Villa",
                description = "Beautifully finished 4-bedroom house with high security, built for comfortable living as part of state-wide luxury developments.",
                pricePerYear = 950000.0,
                address = "New GRA Estate, Maiduguri",
                country = "Nigeria",
                state = "Borno State",
                lga = "Maiduguri",
                city = "Maiduguri",
                street = "Sheriff Crescent",
                rooms = 4,
                propertyType = "House",
                amenities = "Prepaid Meter, Generator House, Pop Ceilings, 24/7 Security Guards",
                imageUrl = "property_villa_4",
                latitude = 11.8311,
                longitude = 13.1510,
                status = "Available"
            )

            propertyDao.insertProperty(p1)
            propertyDao.insertProperty(p2)
            propertyDao.insertProperty(p3)
            propertyDao.insertProperty(p4)

            // Seed a rental request and initial chat message history to show how it works
            val reqId = rentalRequestDao.insertRequest(
                RentalRequest(
                    propertyId = 1L,
                    tenantId = "tenant@afrirent.com",
                    landlordId = "landlord@afrirent.com",
                    durationYears = 2,
                    paymentInterval = "Annually",
                    status = "Accepted",
                    agreementSigned = false,
                    agreementText = "This Long-Term Tenancy Agreement is entered into this day by and between Malam Ibrahim Gwoza (Landlord) and Grace Bitrus (Tenant). The Landlord agrees to let out, and the Tenant agrees to lease the property known as 'Gwoza Town Center Apartment' located at Opposite General Hospital, Bama Road, Gwoza, Borno State, Nigeria, for a term of 2 years."
                )
            )

            chatDao.insertMessage(
                ChatMessage(
                    propertyId = 1L,
                    senderId = "tenant@afrirent.com",
                    receiverId = "landlord@afrirent.com",
                    messageText = "Hello Malam Ibrahim, I am very interested in this 2-bedroom town flat. Is the borehole fully working?",
                    timestamp = System.currentTimeMillis() - 3600000 * 2
                )
            )
            chatDao.insertMessage(
                ChatMessage(
                    propertyId = 1L,
                    senderId = "landlord@afrirent.com",
                    receiverId = "tenant@afrirent.com",
                    messageText = "Ina kwana (Good morning) Grace! Yes, the borehole water system is fully active and powered. It runs daily.",
                    timestamp = System.currentTimeMillis() - 3600000
                )
            )

            // Seed a review for first house
            reviewDao.insertReview(
                Review(
                    propertyId = 1L,
                    reviewerId = "tenant@afrirent.com",
                    revieweeId = "landlord@afrirent.com",
                    rating = 5.0f,
                    comment = "Excellent landlord, very accommodating and the flat water supply is consistent."
                )
            )
        }
    }
}
