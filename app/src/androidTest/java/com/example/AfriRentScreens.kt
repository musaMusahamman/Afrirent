package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import com.example.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.AfriRentViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AfriRentApp(viewModel: AfriRentViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (currentScreen != "auth") {
                AfriRentHeader(viewModel)
            }
        },
        bottomBar = {
            if (currentScreen != "auth" && currentUser != null) {
                AfriRentBottomBar(viewModel)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                }
            ) { target ->
                when (target) {
                    "home" -> HomeScreen(viewModel)
                    "property_detail" -> PropertyDetailScreen(viewModel)
                    "chat" -> ChatScreen(viewModel)
                    "dashboard" -> DashboardScreen(viewModel)
                    "admin" -> AdminDashboardScreen(viewModel)
                    "add_property" -> AddPropertyScreen(viewModel)
                    else -> AuthScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun AfriRentHeader(viewModel: AfriRentViewModel) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = viewModel.translate("app_title"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = viewModel.translate("app_subtitle"),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language Toggle Button
                TextButton(
                    onClick = { viewModel.toggleLanguage() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = lang, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Profile Verification Status Icon
                if (user != null) {
                    val icon = if (user?.isVerified == true) Icons.Default.Verified else Icons.Default.Warning
                    val tint = if (user?.isVerified == true) Color(0xFF4CAF50) else Color(0xFFFFC107)
                    IconButton(onClick = {
                        if (user?.isVerified == false) {
                            viewModel.navigateTo("dashboard")
                        }
                    }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Verification status",
                            tint = tint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AfriRentBottomBar(viewModel: AfriRentViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle() ?: return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == "home" || currentScreen == "property_detail" || currentScreen == "chat" || currentScreen == "add_property",
            onClick = { viewModel.navigateTo("home") },
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
            label = { Text(viewModel.translate("filter")) }
        )

        NavigationBarItem(
            selected = currentScreen == "dashboard",
            onClick = { viewModel.navigateTo("dashboard") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text(viewModel.translate("dashboard")) }
        )

        if (user?.role == "Admin") {
            NavigationBarItem(
                selected = currentScreen == "admin",
                onClick = { viewModel.navigateTo("admin") },
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                label = { Text("Admin") }
            )
        }
    }
}

@Composable
fun AuthScreen(viewModel: AfriRentViewModel) {
    var emailOrPhone by remember { mutableStateOf("") }
    var hasSentOtp by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Tenant") } // "Tenant", "Landlord", "Admin"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        // Styled Brand Header
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HomeWork,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AfriRent",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Gwoza Long-Term Rental Platform",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasSentOtp) "Verify OTP Code" else "Landlord / Tenant Gateway",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (!hasSentOtp) {
                    // Full Name Input
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name (Sunanka / Sunanki)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone/Email Input
                    OutlinedTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        label = { Text("Email or Phone Number") },
                        leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Chooser
                    Text(
                        text = "Select Application Role:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Tenant", "Landlord", "Admin").forEach { role ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRole = role }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRole == role,
                                    onClick = { selectedRole = role }
                                )
                                Text(
                                    text = role,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (emailOrPhone.isNotBlank() && fullName.isNotBlank()) {
                                hasSentOtp = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Send Verification OTP", color = Color.White)
                    }
                } else {
                    // OTP Keypad / Input
                    Text(
                        text = "OTP sent to $emailOrPhone. Enter simulated 4-digit code (eg. '1234') to verify secure link.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 4) otpCode = it },
                        label = { Text("4-Digit OTP Code") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (otpCode.isNotBlank()) {
                                viewModel.authenticate(emailOrPhone, selectedRole, fullName)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Confirm Code & Join AfriRent", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { hasSentOtp = false }) {
                        Text("Edit Mobile Number / Role", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        // Footnote about localized multi-state expansion
        Text(
            text = "Currently servicing Gwoza (Borno State) pilot programs. Multi-state NIN registry encryption powered by Federal Safety Mandates.",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
fun HomeScreen(viewModel: AfriRentViewModel) {
    val properties by viewModel.filteredProperties.collectAsStateWithLifecycle()
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()
    val maxPrice by viewModel.filterPriceMax.collectAsStateWithLifecycle()
    val roomsFilter by viewModel.filterRooms.collectAsStateWithLifecycle()
    val typeFilter by viewModel.filterPropertyType.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }
    var viewModeMap by remember { mutableStateOf(false) } // False = List, True = Interactive Gwoza Map

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome and Role Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = viewModel.translate("welcome", currentUser?.fullName ?: "Resident"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Role: ${currentUser?.role} | Borno State, Nigeria",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Quick Toggle view mode
            Row {
                IconButton(
                    onClick = { viewModeMap = !viewModeMap },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (viewModeMap) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = if (viewModeMap) Icons.Default.FormatListBulleted else Icons.Default.Map,
                        contentDescription = "Toggle View Mode",
                        tint = if (viewModeMap) Color.Black else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (currentUser?.role == "Landlord") {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.navigateTo("add_property") },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddHome,
                            contentDescription = "Post Property",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar with Filters Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text(viewModel.translate("search_placeholder"), fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showFilters = !showFilters },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showFilters) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = if (showFilters) Color.Black else Color.White
                )
            }
        }

        // Expanded Filters Sheet
        AnimatedVisibility(visible = showFilters) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = viewModel.translate("filter_title"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Max Price Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = viewModel.translate("max_price"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₦${maxPrice.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = maxPrice.toFloat(),
                        onValueChange = { viewModel.filterPriceMax.value = it.toDouble() },
                        valueRange = 50000f..1500000f,
                        steps = 29
                    )

                    // Rooms Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = viewModel.translate("rooms"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(0, 1, 2, 3, 4).forEach { roomCount ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (roomsFilter == roomCount) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.filterRooms.value = roomCount },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (roomCount == 0) "Any" else roomCount.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (roomsFilter == roomCount) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Property type filters row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = viewModel.translate("prop_type_label"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyRow {
                            items(listOf("Any", "Apartment", "Bungalow", "House", "Self-Contain")) { type ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .border(
                                            1.dp,
                                            if (typeFilter == type) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (typeFilter == type) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { viewModel.filterPropertyType.value = type }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = type, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Simulated multi-state toggle (Gwoza pilot expansion)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Pilot State: Borno State", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(text = "LGA: Gwoza", fontSize = 11.sp, color = Color.Gray)
                        }
                        TextButton(onClick = { /* Simulated expansion */ }) {
                            Text("Nigerian Expansion States (36)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (viewModeMap) {
            // Interactive custom vector Gwoza Map Canvas!
            MapCanvasView(viewModel, properties)
        } else {
            // Standard Property list
            if (properties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Cabin,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No long-term listings match filters",
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try increasing the budget or modifying the Gwoza criteria.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(properties) { prop ->
                        PropertyCard(prop, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MapCanvasView(viewModel: AfriRentViewModel, properties: List<Property>) {
    var selectedMapProp by remember { mutableStateOf<Property?>(null) }

    // Coordinates mapping constraints for Gwoza Town
    // Long: 13.68 to 13.71, Lat: 11.07 to 11.095
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = viewModel.translate("gwoza_map"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap on interactive ambers below to explore homes offline.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDE8DC)) // Light sand map sand-stone style background
                        .pointerInput(properties) {
                            detectTapGestures { offset ->
                                val width = this.size.width
                                val height = this.size.height
                                // See which property corresponds to the touch offset coordinates map
                                var found: Property? = null
                                for (p in properties) {
                                    val xPx = ((p.longitude - 13.68) / (13.71 - 13.68) * width).toFloat()
                                    val yPx = ((11.095 - p.latitude) / (11.095 - 11.07) * height).toFloat()
                                    val dist = Math.hypot((offset.x - xPx).toDouble(), (offset.y - yPx).toDouble())
                                    if (dist < 36.0) {
                                        found = p
                                        break
                                    }
                                }
                                selectedMapProp = found
                            }
                        }
                ) {
                    // Custom Draw Canvas map
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // 1. Draw Gwoza Hills as Background Curves
                        val hillPath = Path().apply {
                            moveTo(0f, h * 0.7f)
                            quadraticTo(w * 0.25f, h * 0.4f, w * 0.5f, h * 0.65f)
                            quadraticTo(w * 0.75f, h * 0.35f, w, h * 0.6f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(hillPath, color = Color(0xFFDFD8C9))

                        // Draw secondary smaller hill
                        val hillPath2 = Path().apply {
                            moveTo(0f, h * 0.85f)
                            quadraticTo(w * 0.4f, h * 0.62f, w * 0.75f, h * 0.82f)
                            quadraticTo(w * 0.9f, h * 0.72f, w, h * 0.78f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(hillPath2, color = Color(0xFFD6CEB8))

                        // 2. Draw Gwoza Outer Bounds and Pilot Roads
                        // Bama Main Road (Slash diagonal across)
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, h * 0.15f),
                            end = Offset(w, h * 0.9f),
                            strokeWidth = 14f
                        )
                        drawLine(
                            color = Color(0xFFA5A5A5),
                            start = Offset(0f, h * 0.15f),
                            end = Offset(w, h * 0.9f),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Secretariat Link (Vertical cross)
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.45f, 0f),
                            end = Offset(w * 0.52f, h),
                            strokeWidth = 10f
                        )

                        // Lowcost Lane
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, h * 0.6f),
                            end = Offset(w * 0.45f, h * 0.6f),
                            strokeWidth = 10f
                        )

                        // Draw Landmark Text overlays securely on map coordinates
                        drawContext.canvas.nativeCanvas.apply {
                            // Can add details securely
                        }

                        // 3. Draw Properties as active Markers on Canvas
                        properties.forEach { p ->
                            val xPx = ((p.longitude - 13.68) / (13.71 - 13.68) * w).toFloat()
                            val yPx = ((11.095 - p.latitude) / (11.095 - 11.07) * h).toFloat()

                            // Draw a small drop shadow for pin
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.25f),
                                radius = 20f,
                                center = Offset(xPx, yPx + 4f)
                            )

                            // Main Pin circle: Gold for Available, Coral for Rented/Unavailable
                            val pinBg = if (p.status == "Available") NigerGreenPrimary else NigerCoralAccent
                            drawCircle(
                                color = pinBg,
                                radius = 16f,
                                center = Offset(xPx, yPx)
                            )

                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = Offset(xPx, yPx)
                            )
                        }
                    }

                    // Render dynamic tooltip overlay on active selection
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedMapProp != null,
                        enter = fadeIn() + expandIn(),
                        exit = fadeOut() + shrinkOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp)
                    ) {
                        val prop = selectedMapProp
                        if (prop != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.navigateTo("property_detail", prop.id) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prop.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "₦${prop.pricePerYear.toInt()}/yr • ${prop.rooms} rooms",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = prop.address,
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = "Details",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PropertyCard(property: Property, viewModel: AfriRentViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.navigateTo("property_detail", property.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Visual Banner with Local/Dynamic Illustration placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            ) {
                // Vector background pattern details
                Icon(
                    imageVector = Icons.Default.Villa,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(110.dp)
                        .align(Alignment.Center)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = property.propertyType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Price Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "₦" + property.pricePerYear.toInt().toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }

                // Bottom strip showing availability
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (property.status == "Available") Color(0xFF4CAF50) else Color(0xFFFF5722)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = viewModel.translate(if (property.status == "Available") "available" else "rented"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Text info
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = property.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.address}, ${property.city}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Divider(color = Color.LightGray.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hotel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${property.rooms} ${viewModel.translate("rooms")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "4.8 (Owner Verified)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PropertyDetailScreen(viewModel: AfriRentViewModel) {
    val prop by viewModel.selectedProperty.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val reviews by viewModel.selectedPropertyReviews.collectAsStateWithLifecycle()

    var showApplicationSheet by remember { mutableStateOf(false) }
    var applicationDuration by remember { mutableStateOf(1) } // 1, 2, or 3 years
    var applicationSchedule by remember { mutableStateOf("Annually") } // "Annually" or "Monthly"

    var landlordReviewedUser by remember { mutableStateOf<User?>(null) }
    var reviewRatingText by remember { mutableStateOf("5.0") }
    var reviewCommentText by remember { mutableStateOf("") }

    if (prop == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentProp = prop!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.goBack() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(4.dp))
                Text(viewModel.translate("back"))
            }
        }

        // Hero Image illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Villa,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "PILOT LISTING: GWOZA",
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
            )
        }

        // Details Wrap
        Column(modifier = Modifier.padding(16.dp)) {
            // Price & Title block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentProp.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${currentProp.rooms} rooms • ${currentProp.propertyType} • ${currentProp.state}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "₦" + currentProp.pricePerYear.toInt().toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Address and verification block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NigerGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Secure Tenancy Protected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Backed by BVN/NIN identity approvals at contract signing.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map placement view reminder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PinDrop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentProp.address,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amenities List Chips
            Text(
                text = viewModel.translate("amenities"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                currentProp.amenities.split(",").forEach { amenity ->
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = amenity.trim(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description block
            Text(
                text = viewModel.translate("description"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = currentProp.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons based on verification
            if (user != null) {
                if (user?.isVerified == true) {
                    if (currentProp.status == "Available") {
                        Button(
                            onClick = { showApplicationSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.RequestQuote, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.translate("request_lease"), color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Property is currently leased out (Rented)")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.navigateTo("chat", currentProp.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(viewModel.translate("contact_owner"))
                    }
                } else {
                    // Force NIN identity verification
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🔒 Verification Required",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "To lease properties in Gwoza, Federal rental policies require you verify your identity with official BVN or NIN document first.",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Button(
                                onClick = { viewModel.navigateTo("dashboard") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Go Verify Profile", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lease Application Drawer/Popup Sheet Custom implementation
            AnimatedVisibility(visible = showApplicationSheet) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = viewModel.translate("request_lease"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Duration chooser
                        Text(text = viewModel.translate("lease_duration") + ":", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            listOf(1, 2, 3).forEach { year ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .border(
                                            1.dp,
                                            if (applicationDuration == year) MaterialTheme.colorScheme.secondary else Color.LightGray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(if (applicationDuration == year) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { applicationDuration = year }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$year year(s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Schedule chooser
                        Text(text = viewModel.translate("payment_schedule") + ":", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            listOf("Annually", "Monthly").forEach { sched ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .border(
                                            1.dp,
                                            if (applicationSchedule == sched) MaterialTheme.colorScheme.secondary else Color.LightGray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(if (applicationSchedule == sched) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { applicationSchedule = sched }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (sched == "Annually") viewModel.translate("schedule_annual") else viewModel.translate("schedule_monthly"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Total Pricing Estimation
                        val priceNum = currentProp.pricePerYear
                        val totalBill = priceNum * applicationDuration
                        val firstCommence = if (applicationSchedule == "Annually") totalBill else (priceNum / 12)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Contract Total Value:", fontSize = 11.sp)
                                    Text("₦${totalBill.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("First commitment payment due:", fontSize = 11.sp)
                                    Text("₦${firstCommence.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NigerGreenPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.applyForLease(applicationDuration, applicationSchedule)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(viewModel.translate("submit_lease_request"), color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reviews Display list
            Text(
                text = viewModel.translate("reviews") + " (${reviews.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (reviews.isEmpty()) {
                Text(
                    text = "No user ratings yet for this landlord. Safe reviews can only be made after lease activation.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                reviews.forEach { r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (r.reviewerId.contains("system")) "AfriRent Secure Ledger" else "Verified Tenant ID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    repeat(r.rating.toInt()) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = r.comment,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Review input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Raba Sharhinka (Write Review):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = reviewCommentText,
                            onValueChange = { reviewCommentText = it },
                            placeholder = { Text("E.g. Malam Ibrahim provides excellent secure solar backup...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (reviewCommentText.isNotBlank()) {
                                    val rtValue = reviewRatingText.toFloatOrNull() ?: 5.0f
                                    viewModel.submitReview(rtValue, reviewCommentText)
                                    reviewCommentText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: AfriRentViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val property by viewModel.selectedProperty.collectAsStateWithLifecycle()

    var typedText by remember { mutableStateOf("") }

    if (user == null || property == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val me = user!!
    val currentProp = property!!

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.goBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = currentProp.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Secure Link (Contact Owner masked)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Masked phone number warnings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛡️ " + viewModel.translate("phone_hidden_notice"),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        // Chat bubble lists
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == me.id
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                )
                            )
                            .background(
                                if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = msg.messageText,
                                fontSize = 12.sp,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Secure Connection",
                                fontSize = 8.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.6f) else Color.Gray,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Messenger typing panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = typedText,
                onValueChange = { typedText = it },
                placeholder = { Text(viewModel.translate("chat_placeholder"), fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (typedText.isNotBlank()) {
                        viewModel.sendChatMessage(typedText)
                        typedText = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: AfriRentViewModel) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val requests by viewModel.userRentalRequests.collectAsStateWithLifecycle()
    val properties by viewModel.allProperties.collectAsStateWithLifecycle()
    val reminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val receipts by viewModel.allReceipts.collectAsStateWithLifecycle()

    var inputNinBvn by remember { mutableStateOf("") }
    var gateSelector by remember { mutableStateOf("Paystack") } // "Paystack" or "Flutterwave"
    var showCheckoutForRequest by remember { mutableStateOf<Long?>(null) } // holds active rental request ID for checkout

    // Checkout credit card simulated typing helper
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var pinSimulated by remember { mutableStateOf("") }
    var isProcessingPayment by remember { mutableStateOf(false) }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val me = user!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // User Identity Summary & Verification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = me.fullName, fontWeight = FontWeight.Bold)
                            Text(text = me.email, fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (me.isVerified) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = viewModel.translate(if (me.isVerified) "verified_badge" else "unverified_badge"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (me.isVerified) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!me.isVerified) {
                    Text(
                        text = viewModel.translate("verify_id"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = viewModel.translate("verify_desc"),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = inputNinBvn,
                        onValueChange = { if (it.length <= 11) inputNinBvn = it },
                        placeholder = { Text(viewModel.translate("nin_placeholder")) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (inputNinBvn.length == 11) {
                                viewModel.verifyIdentity(inputNinBvn)
                                inputNinBvn = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(viewModel.translate("submit_auth"), color = Color.White)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Identity Registered:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = me.idNumber, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        }
                        TextButton(onClick = { viewModel.unverifyCurrentUser() }) {
                            Text("Reset Identity Documents", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notifications Center (Reminder system)
        if (me.role == "Tenant") {
            Text(
                text = "Notifications & Reminders",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (reminders.isEmpty()) {
                Text(
                    text = "No pending notifications. You will be reminded 7 days, 3 days, and 1 day before any rent starts/expires.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                reminders.forEach { notificationMsg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = notificationMsg,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Long-term Leases & Applications Section
        Text(
            text = if (me.role == "Tenant") viewModel.translate("my_rentals") else "Tenant Applications for My Properties",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No lease contract activity found.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            requests.forEach { req ->
                val prop = properties.find { p -> p.id == req.propertyId }
                if (prop != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = prop.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (req.status) {
                                                "Paid" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                "Accepted" -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                                "Pending" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                else -> Color.LightGray
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = req.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (req.status) {
                                            "Paid" -> Color(0xFF2E7D32)
                                            "Accepted" -> Color(0xFF1565C0)
                                            "Pending" -> Color(0xFFEF6C00)
                                            else -> Color.DarkGray
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = prop.address, fontSize = 11.sp, color = Color.Gray)
                            Text(text = "Price: ₦${prop.pricePerYear.toInt()}/yr • Term: ${req.durationYears} year(s)", fontSize = 11.sp)

                            Spacer(modifier = Modifier.height(12.dp))

                            if (me.role == "Landlord" && req.status == "Pending") {
                                Row {
                                    Button(
                                        onClick = { viewModel.respondToRentalRequest(req.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NigerGreenPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Approve Application", color = Color.White, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.respondToRentalRequest(req.id, false) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (me.role == "Tenant" && req.status == "Accepted") {
                                if (!req.agreementSigned) {
                                    // Tenancy Signer contract visual screen
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF6)), // Paper theme
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = viewModel.translate("digital_agreement"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = req.agreementText,
                                                fontSize = 10.sp,
                                                maxLines = 5,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Button(
                                                onClick = { viewModel.signDigitalAgreement(req.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Draw, contentDescription = null, tint = Color.Black)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = viewModel.translate("sign_now"),
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Contract is fully signed! Let's pay!
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = NigerGreenPrimary.copy(alpha = 0.05f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "📜 " + viewModel.translate("signed"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NigerGreenPrimary
                                            )
                                            Text(
                                                text = "Digital Tenancy fully verified. Complete payment commencement.",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )

                                            Button(
                                                onClick = { showCheckoutForRequest = req.id },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                            ) {
                                                Text(viewModel.translate("pay_now"), color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Gate Payment Checkout Overlay Panel (Paystack & Flutterwave Simulator)
        if (showCheckoutForRequest != null) {
            val reqId = showCheckoutForRequest!!
            val req = requests.find { it.id == reqId }
            val prop = properties.find { p -> p.id == (req?.propertyId ?: 0L) }

            if (req != null && prop != null) {
                val priceNum = prop.pricePerYear
                val rentPeriodBill = if (req.paymentInterval == "Annually") priceNum * req.durationYears else (priceNum / 12)

                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Checkout Gateway Simulator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Simulating secure Paystack / Flutterwave authorization flow",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Choose Gateway Toggle Tabs
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Paystack", "Flutterwave").forEach { gateway ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .border(
                                            1.dp,
                                            if (gateSelector == gateway) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(if (gateSelector == gateway) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { gateSelector = gateway }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(gateway, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Amount: ₦${rentPeriodBill.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NigerGreenPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulated Card Input Form fields
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            placeholder = { Text("5061 ---- ---- ---- (Verve/Visa Nigeria)") },
                            label = { Text("Card Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { cardExpiry = it },
                                placeholder = { Text("MM/YY") },
                                label = { Text("Expiry Date") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = cardCvv,
                                onValueChange = { cardCvv = it },
                                placeholder = { Text("CVV") },
                                label = { Text("CVV") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pinSimulated,
                            onValueChange = { pinSimulated = it },
                            placeholder = { Text("Simulated Bank Pin") },
                            label = { Text("Card Pin") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isProcessingPayment) {
                            CircularProgressIndicator()
                            Text("Authorizing Paystack Ledger securely...", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        } else {
                            Button(
                                onClick = {
                                    if (cardNumber.isNotBlank() && cardExpiry.isNotBlank() && cardCvv.isNotBlank() && pinSimulated.isNotBlank()) {
                                        isProcessingPayment = true
                                        // mock 2 seconds processing
                                        viewModel.processSimulatedPayment(reqId, gateSelector)
                                        showCheckoutForRequest = null
                                        isProcessingPayment = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Complete Verification Pay", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { showCheckoutForRequest = null }) {
                            Text("Cancel payment")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Receipt Archive displays
        if (receipts.isNotEmpty()) {
            Text(
                text = "Archived Payment Receipts (PDF Saved)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            receipts.forEach { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = NigerGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = rec.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = "Amount: ₦${rec.amount.toInt()} • Method: ${rec.paymentGateway}", fontSize = 11.sp)
                            }
                        }

                        IconButton(onClick = { /* simulated share print */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Share receipt", tint = NigerGreenPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(viewModel: AfriRentViewModel) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val properties by viewModel.allProperties.collectAsStateWithLifecycle()
    val receipts by viewModel.allReceipts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "AfriRent Admin Oversight Hub",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Metrics Row cards
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                colors = CardDefaults.cardColors(containerColor = NigerGreenPrimary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Members", fontSize = 11.sp)
                    Text("${users.size}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NigerGreenPrimary)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Listed Houses", fontSize = 11.sp)
                    Text("${properties.size}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Secure Vol (₦)", fontSize = 11.sp)
                    val sum = receipts.sumOf { it.amount }
                    Text("₦${sum.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security Identity Approvals (NIN / BVN verification list)
        Text(
            text = "Pending Identity Document Approvals",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val unverifiedUsers = users.filter { !it.isVerified }
        if (unverifiedUsers.isEmpty()) {
            Text(
                text = "All active Gwoza Landlords have completed automated security ledger approvals.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            unverifiedUsers.forEach { u ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = u.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "ID Required: " + if (u.role == "Landlord") "Landlord License File" else "Tenant Profile", fontSize = 11.sp, color = Color.Gray)
                            }

                            Button(
                                onClick = { viewModel.approveUserIdentityAndRefresh(u.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Approve Security ID", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Disputes Ledger
        Text(
            text = viewModel.translate("disputes"),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.translate("no_disputes"),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AddPropertyScreen(viewModel: AfriRentViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pricePerYear by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var roomsCount by remember { mutableStateOf("2") }
    var selectedType by remember { mutableStateOf("Apartment") } // "Apartment", "House", "Bungalow", "Self-Contain"
    var amenities by remember { mutableStateOf("Water Running, Solar Backup, Secured Yard") }

    var selectedState by remember { mutableStateOf("Borno State") }
    var selectedLga by remember { mutableStateOf("Gwoza") }
    var selectedCity by remember { mutableStateOf("Gwoza Town") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.goBack() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(4.dp))
                Text(viewModel.translate("back"))
            }
        }

        Text(
            text = "Publish Long-Term House Listing",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Listing Title (e.g., Gwoza Sand-Stone Bungalow)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("About this property (Description)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = pricePerYear,
            onValueChange = { pricePerYear = it },
            label = { Text("Rental Price Per Year (₦)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Street Address (e.g., Block B, Lowcost Estate)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-country / Multi-state expansion fields pre-loaded
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedState,
                onValueChange = { selectedState = it },
                label = { Text("State") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            OutlinedTextField(
                value = selectedLga,
                onValueChange = { selectedLga = it },
                label = { Text("LGA") },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCity,
                onValueChange = { selectedCity = it },
                label = { Text("City/Town") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            OutlinedTextField(
                value = roomsCount,
                onValueChange = { roomsCount = it },
                label = { Text("Rooms Count") },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Property type selector
        Text(text = "Select Property Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Apartment", "Bungalow", "House", "Self-Contain").forEach { pType ->
                Box(
                    modifier = Modifier
                        .clickable { selectedType = pType }
                        .border(
                            1.dp,
                            if (selectedType == pType) MaterialTheme.colorScheme.primary else Color.LightGray,
                            RoundedCornerShape(8.dp)
                        )
                        .background(if (selectedType == pType) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = pType, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = amenities,
            onValueChange = { amenities = it },
            label = { Text("Amenities List (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && pricePerYear.isNotBlank()) {
                    viewModel.publishProperty(
                        title = title,
                        description = description,
                        priceStr = pricePerYear,
                        address = address,
                        rooms = roomsCount.toIntOrNull() ?: 2,
                        type = selectedType,
                        amenities = amenities,
                        state = selectedState,
                        lga = selectedLga,
                        city = selectedCity
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Publish & Save Securely", color = Color.White)
        }
    }
}
