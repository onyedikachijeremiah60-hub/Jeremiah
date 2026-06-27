package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun getDrawableIdByName(name: String): Int {
    return when (name) {
        "jay_trendz_logo" -> R.drawable.jay_trendz_logo
        "mens_wear" -> R.drawable.mens_wear
        "womens_wear" -> R.drawable.womens_wear
        "accessories" -> R.drawable.accessories
        else -> R.drawable.jay_trendz_logo
    }
}

@Composable
fun ProductImage(
    imageSource: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap = remember(imageSource) {
        if (imageSource.startsWith("/") || imageSource.startsWith("content://") || imageSource.startsWith("file:")) {
            try {
                val file = java.io.File(imageSource)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    val uri = Uri.parse(imageSource)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Image(
            painter = painterResource(id = getDrawableIdByName(imageSource)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

fun saveUriToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "product_img_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JayTrendzApp(viewModel: JayTrendzViewModel) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartState.collectAsStateWithLifecycle()
    val currentScreen = viewModel.currentScreen
    val context = LocalContext.current

    // Observe orders for potential toast dispatch notifications
    val orders by viewModel.ordersState.collectAsStateWithLifecycle()
    var lastKnownStatus by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(orders) {
        orders.forEach { order ->
            val prevStatus = lastKnownStatus[order.id]
            if (prevStatus != null && prevStatus != order.status) {
                Toast.makeText(
                    context,
                    "📦 Order ${order.id} update: ${order.status}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        lastKnownStatus = orders.associate { it.id to it.status }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LuxuryBlack,
        bottomBar = {
            if (currentScreen != Screen.Register && currentScreen !is Screen.Payment) {
                JayTrendzBottomBar(
                    currentScreen = currentScreen,
                    cartCount = cartItems.sumOf { it.quantity },
                    onNavigate = { screen ->
                        viewModel.navigateTo(screen)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    is Screen.Register -> RegisterScreen(viewModel)
                    is Screen.Catalog -> CatalogScreen(viewModel)
                    is Screen.ProductDetails -> ProductDetailsScreen(targetScreen.product, viewModel)
                    is Screen.Cart -> CartScreen(viewModel)
                    is Screen.Checkout -> CheckoutScreen(viewModel)
                    is Screen.Payment -> PaymentScreen(
                        targetScreen.orderId,
                        targetScreen.totalAmount,
                        targetScreen.shippingFee,
                        viewModel
                    )
                    is Screen.Tracker -> TrackerScreen(targetScreen.orderId, viewModel)
                    is Screen.SupportHub -> SupportHubScreen(viewModel)
                    is Screen.Profile -> ProfileScreen(viewModel)
                    is Screen.UploadProduct -> UploadProductScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun JayTrendzBottomBar(
    currentScreen: Screen,
    cartCount: Int,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F0F12),
        tonalElevation = 8.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = LuxuryBorderGrey,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        )
    ) {
        val homeSelected = currentScreen is Screen.Catalog || currentScreen is Screen.ProductDetails
        NavigationBarItem(
            selected = homeSelected,
            onClick = { onNavigate(Screen.Catalog) },
            icon = {
                Icon(
                    imageVector = if (homeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home",
                    tint = if (homeSelected) LuxuryGold else LuxuryTextGrey
                )
            },
            label = {
                Text(
                    "Home",
                    color = if (homeSelected) LuxuryGold else LuxuryTextGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFF202025)
            ),
            modifier = Modifier.testTag("nav_home")
        )

        val cartSelected = currentScreen is Screen.Cart || currentScreen is Screen.Checkout
        NavigationBarItem(
            selected = cartSelected,
            onClick = { onNavigate(Screen.Cart) },
            icon = {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = LuxuryGold,
                                contentColor = LuxuryBlack
                            ) {
                                Text(
                                    cartCount.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (cartSelected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                        contentDescription = "Cart",
                        tint = if (cartSelected) LuxuryGold else LuxuryTextGrey
                    )
                }
            },
            label = {
                Text(
                    "Cart",
                    color = if (cartSelected) LuxuryGold else LuxuryTextGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFF202025)
            ),
            modifier = Modifier.testTag("nav_cart")
        )

        val supportSelected = currentScreen is Screen.SupportHub
        NavigationBarItem(
            selected = supportSelected,
            onClick = { onNavigate(Screen.SupportHub) },
            icon = {
                Icon(
                    imageVector = if (supportSelected) Icons.Filled.QuestionAnswer else Icons.Outlined.QuestionAnswer,
                    contentDescription = "Support",
                    tint = if (supportSelected) LuxuryGold else LuxuryTextGrey
                )
            },
            label = {
                Text(
                    "Support",
                    color = if (supportSelected) LuxuryGold else LuxuryTextGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFF202025)
            ),
            modifier = Modifier.testTag("nav_support")
        )

        val profileSelected = currentScreen is Screen.Profile || currentScreen is Screen.Tracker
        NavigationBarItem(
            selected = profileSelected,
            onClick = { onNavigate(Screen.Profile) },
            icon = {
                Icon(
                    imageVector = if (profileSelected) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = if (profileSelected) LuxuryGold else LuxuryTextGrey
                )
            },
            label = {
                Text(
                    "Profile",
                    color = if (profileSelected) LuxuryGold else LuxuryTextGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFF202025)
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}

// BOLD TYPOGRAPHY IMPLEMENTATIONS

@Composable
fun LuxuryHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F12))
            .border(width = 1.dp, color = LuxuryBorderGrey)
            .padding(horizontal = 16.0.dp, vertical = 18.0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1D1D22), CircleShape)
                        .border(1.dp, LuxuryBorderGrey, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LuxuryWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    fontStyle = FontStyle.Italic
                ),
                color = LuxuryGold,
                modifier = Modifier.weight(1f)
            )

            if (actions != null) {
                Row(content = actions)
            }
        }
    }
}

@Composable
fun BrandingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LuxuryGold)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SHIPPING FROM EHIME MBANO HUB, IMO STATE • NATIONWIDE DELIVERY • NIGERIA",
            color = LuxuryBlack,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RegisterScreen(viewModel: JayTrendzViewModel) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var house by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("Imo") }
    var isStateMenuExpanded by remember { mutableStateOf(false) }

    val nigerianStates = listOf(
        "Imo", "Abia", "Anambra", "Enugu", "Ebonyi", "Rivers", 
        "Lagos", "Abuja", "Oyo", "Kano", "Kaduna", "Delta", "Edo"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // Aesthetic Top Logo section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF201A09), LuxuryBlack)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.jay_trendz_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, LuxuryGold, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "JAY TRENDZ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    fontStyle = FontStyle.Italic,
                    color = LuxuryGold
                )
                Text(
                    text = "PREMIUM NIGERIAN FASHION HUB",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = LuxuryWhite.copy(alpha = 0.7f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CREATE AN ACCOUNT",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = LuxuryWhite
            )

            Text(
                text = "To proceed with delivery orders, configure your exact Nigeria doorstep delivery details.",
                fontSize = 12.sp,
                color = LuxuryTextGrey
            )

            // Inputs
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedLabelColor = LuxuryGold,
                    unfocusedLabelColor = LuxuryTextGrey
                ),
                modifier = Modifier.fillMaxWidth().testTag("reg_fullname")
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedLabelColor = LuxuryGold,
                    unfocusedLabelColor = LuxuryTextGrey
                ),
                modifier = Modifier.fillMaxWidth().testTag("reg_email")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedLabelColor = LuxuryGold,
                    unfocusedLabelColor = LuxuryTextGrey
                ),
                modifier = Modifier.fillMaxWidth().testTag("reg_password")
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (Delivery Coordination)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedLabelColor = LuxuryGold,
                    unfocusedLabelColor = LuxuryTextGrey
                ),
                modifier = Modifier.fillMaxWidth().testTag("reg_phone")
            )

            Divider(color = LuxuryBorderGrey, modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "DELIVERY ADDRESS (NIGERIA ONLY)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LuxuryGold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = house,
                    onValueChange = { house = it },
                    label = { Text("House/Apt No.") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedLabelColor = LuxuryGold,
                        unfocusedLabelColor = LuxuryTextGrey
                    ),
                    modifier = Modifier.weight(1f).testTag("reg_house")
                )

                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Street Address") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedLabelColor = LuxuryGold,
                        unfocusedLabelColor = LuxuryTextGrey
                    ),
                    modifier = Modifier.weight(2.0f).testTag("reg_street")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedLabelColor = LuxuryGold,
                        unfocusedLabelColor = LuxuryTextGrey
                    ),
                    modifier = Modifier.weight(1f).testTag("reg_city")
                )

                // State Dropdown (Nigeria States)
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { isStateMenuExpanded = true },
                        border = BorderStroke(1.dp, LuxuryBorderGrey),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxuryWhite),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("reg_state_selector")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedState, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }

                    DropdownMenu(
                        expanded = isStateMenuExpanded,
                        onDismissRequest = { isStateMenuExpanded = false },
                        modifier = Modifier.background(LuxuryDarkGrey)
                    ) {
                        nigerianStates.forEach { stateName ->
                            DropdownMenuItem(
                                text = { Text(stateName, color = LuxuryWhite) },
                                onClick = {
                                    selectedState = stateName
                                    isStateMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (fullName.isBlank() || email.isBlank() || phone.isBlank() || street.isBlank() || city.isBlank()) {
                        Toast.makeText(context, "Please enter your Name, Email, Phone, and precise Delivery Address so we know where to deliver your order!", Toast.LENGTH_LONG).show()
                    } else if (email.trim().lowercase() == "onyedikachijeremiah60@gmail.com" && !fullName.trim().equals("Onyedikachi Jeremiah", ignoreCase = true)) {
                        Toast.makeText(context, "Access Denied: This email address is reserved exclusively for the Owner & Admin of Jay Trendz.", Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.registerUser(
                            fullName = fullName,
                            email = email.trim(),
                            phone = phone,
                            street = street,
                            house = house,
                            city = city,
                            state = selectedState
                        )
                        Toast.makeText(context, "🎉 Welcome to Jay Trendz, $fullName!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("reg_submit_btn")
            ) {
                Text(
                    text = "REGISTER & JOIN EXCLUSIVE CLUB",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            OutlinedButton(
                onClick = {
                    viewModel.registerUser(
                        fullName = "Onyedikachi Jeremiah",
                        email = "onyedikachijeremiah60@gmail.com",
                        phone = "09042128365",
                        street = "Umualuwaka in Umueze 1",
                        house = "",
                        city = "Ehime Mbano",
                        state = "Imo"
                    )
                },
                border = BorderStroke(1.dp, LuxuryGold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxuryGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = "Admin", tint = LuxuryGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOG IN AS ADMIN / OWNER", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            TextButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("BROWSE CATALOG AS GUEST", color = LuxuryGold, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        BrandingFooter()
    }
}

@Composable
fun CatalogScreen(viewModel: JayTrendzViewModel) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current
    var isFilterDialogOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LuxuryBlack)
        ) {
        // Luxury Header
        LuxuryHeader(
            title = "JAY TRENDZ",
            actions = {
                // Profile/Login indicator
                IconButton(
                    onClick = {
                        if (user?.isLoggedIn == true) {
                            viewModel.navigateTo(Screen.Profile)
                        } else {
                            viewModel.navigateTo(Screen.Register)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1D1D22), CircleShape)
                        .border(1.dp, LuxuryBorderGrey, CircleShape)
                ) {
                    Icon(
                        imageVector = if (user?.isLoggedIn == true) Icons.Filled.Person else Icons.Outlined.Lock,
                        contentDescription = "Profile",
                        tint = if (user?.isLoggedIn == true) LuxuryGold else LuxuryWhite
                    )
                }
            }
        )

        // Custom search and filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = { Text("Search elegant outfits...", color = LuxuryTextGrey) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = LuxuryTextGrey) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedTextColor = LuxuryWhite,
                    unfocusedTextColor = LuxuryWhite
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("catalog_search")
            )

            IconButton(
                onClick = { isFilterDialogOpen = true },
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFF1A1A1E), RoundedCornerShape(8.dp))
                    .border(1.dp, LuxuryBorderGrey, RoundedCornerShape(8.dp))
                    .testTag("catalog_filter_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = LuxuryGold
                )
            }
        }

        // Category Horizontal Flow
        val categories = listOf("All", "Men's Wear", "Women's Wear", "Accessories")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = viewModel.selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) LuxuryGold else Color(0xFF1A1A1E))
                        .border(
                            1.dp,
                            if (isSelected) LuxuryGold else LuxuryBorderGrey,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.selectedCategory = category }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.uppercase(),
                        color = if (isSelected) LuxuryBlack else LuxuryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Main Catalog Listing
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = "No Products",
                        tint = LuxuryGold,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (user?.email == "onyedikachijeremiah60@gmail.com") {
                        Text(
                            text = "WELCOME TO YOUR ADMIN STORE",
                            color = LuxuryGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your premium fashion boutique is currently empty. As the Admin and Owner, you have complete control over this catalog.",
                            color = LuxuryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.UploadProduct) },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Upload")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UPLOAD YOUR FIRST FASHION ITEM", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        Text(
                            text = "COLLECTION COMING SOON",
                            color = LuxuryGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Onyedikachi Jeremiah is preparing exclusive luxury apparel and accessory designs. Please check back shortly for our premium catalog releases!",
                            color = LuxuryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { product ->
                    ProductCard(
                        product = product,
                        onClick = { viewModel.navigateTo(Screen.ProductDetails(product)) }
                    )
                }
            }
        }

        // Direct Helpline Call and Dispatch info banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151518))
                .border(1.dp, LuxuryBorderGrey)
                .clickable {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09042128365"))
                    context.startActivity(intent)
                }
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Helpline",
                    tint = LuxuryGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "DIRECT DISPATCH HELPLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = LuxuryWhite
                    )
                    Text(
                        "Tap to call: 09042128365",
                        fontSize = 10.sp,
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Call",
                tint = LuxuryTextGrey
            )
        }

        BrandingFooter()
        }

        FloatingActionButton(
            onClick = { viewModel.navigateTo(Screen.UploadProduct) },
            containerColor = LuxuryGold,
            contentColor = LuxuryBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp)
                .testTag("fab_add_product")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload Apparel")
        }
    }

    // Filter Dialog
    if (isFilterDialogOpen) {
        Dialog(onDismissRequest = { isFilterDialogOpen = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LuxuryDarkGrey,
                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "FILTER COLLECTION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = LuxuryGold,
                        letterSpacing = 1.sp
                    )

                    Divider(color = LuxuryBorderGrey)

                    Text(
                        "MAX PRICE: ₦${viewModel.maxPriceFilter.toInt()}",
                        fontWeight = FontWeight.Bold,
                        color = LuxuryWhite,
                        fontSize = 14.sp
                    )

                    Slider(
                        value = viewModel.maxPriceFilter.toFloat(),
                        onValueChange = { viewModel.maxPriceFilter = it.toDouble() },
                        valueRange = 10000f..60000f,
                        colors = SliderDefaults.colors(
                            thumbColor = LuxuryGold,
                            activeTrackColor = LuxuryGold,
                            inactiveTrackColor = LuxuryBorderGrey
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.maxPriceFilter = 60000.0
                                viewModel.selectedCategory = "All"
                                isFilterDialogOpen = false
                            }
                        ) {
                            Text("RESET", color = LuxuryTextGrey, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { isFilterDialogOpen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack)
                        ) {
                            Text("APPLY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LuxuryBorderGrey)
    ) {
        Column {
            Box {
                ProductImage(
                    imageSource = product.imageDrawableName,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )

                // Premium Category Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(LuxuryBlack.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .border(1.dp, LuxuryGold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = product.category.uppercase(),
                        color = LuxuryGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = product.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LuxuryWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.description,
                    fontSize = 12.sp,
                    color = LuxuryTextGrey,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₦${String.format("%,.0f", product.price)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxuryGold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Quality",
                            tint = LuxuryGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PREMIUM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = LuxuryWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailsScreen(product: Product, viewModel: JayTrendzViewModel) {
    val context = LocalContext.current
    var selectedSize by remember { mutableStateOf(product.sizes.split(",").firstOrNull()?.trim() ?: "") }
    var selectedColor by remember { mutableStateOf(product.colors.split(",").firstOrNull()?.trim() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = "ITEM DETAILS",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ProductImage(
                imageSource = product.imageDrawableName,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = LuxuryWhite
                        )
                        Text(
                            text = product.category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuxuryGold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "₦${String.format("%,.0f", product.price)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxuryGold
                    )
                }

                Divider(color = LuxuryBorderGrey)

                Text(
                    text = "DESCRIPTION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = LuxuryGold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = product.description,
                    fontSize = 14.sp,
                    color = LuxuryWhite,
                    lineHeight = 20.sp
                )

                // Size Picker
                if (product.sizes.isNotEmpty() && product.sizes != "Standard" && product.sizes != "Adjustable" && product.sizes != "Free Size") {
                    Text(
                        text = "CHOOSE SIZE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = LuxuryGold,
                        letterSpacing = 1.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(product.sizes.split(",")) { sizeRaw ->
                            val s = sizeRaw.trim()
                            val isChosen = selectedSize == s
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(if (isChosen) LuxuryGold else Color(0xFF1E1E24))
                                    .border(
                                        1.dp,
                                        if (isChosen) LuxuryGold else LuxuryBorderGrey,
                                        CircleShape
                                    )
                                    .clickable { selectedSize = s },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s,
                                    color = if (isChosen) LuxuryBlack else LuxuryWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Color Picker
                Text(
                    text = "CHOOSE COLOR ACCENT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = LuxuryGold,
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(product.colors.split(",")) { colorRaw ->
                        val c = colorRaw.trim()
                        val isChosen = selectedColor == c
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) LuxuryGold else Color(0xFF1E1E24))
                                .border(
                                    1.dp,
                                    if (isChosen) LuxuryGold else LuxuryBorderGrey,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedColor = c }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = c,
                                color = if (isChosen) LuxuryBlack else LuxuryWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.addToCart(product, selectedSize, selectedColor)
                        Toast.makeText(context, "Added to shopping bag!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("add_to_cart_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Bag")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "ADD TO SHOPPING BAG",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.startNegotiation(product)
                    },
                    border = BorderStroke(1.dp, LuxuryGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxuryGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("chat_owner_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = LuxuryGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "CHAT WITH OWNER TO BUY",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        BrandingFooter()
    }
}

@Composable
fun CartScreen(viewModel: JayTrendzViewModel) {
    val cartItems by viewModel.cartState.collectAsStateWithLifecycle()
    val user by viewModel.userState.collectAsStateWithLifecycle()

    val subtotal = cartItems.sumOf { it.productPrice * it.quantity }
    val shippingFee = if (user?.isLoggedIn == true) viewModel.calculateShippingFee(user!!.state) else 0.0
    val total = subtotal + shippingFee

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = "YOUR SHOPPING BAG"
        )

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = "Empty Bag",
                        tint = LuxuryTextGrey,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your luxury bag is empty.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxuryWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Browse catalog to add exclusive styles.",
                        color = LuxuryTextGrey,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Catalog) },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack)
                    ) {
                        Text("START SHOPPING", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems) { item ->
                        CartItemRow(item, viewModel)
                    }
                }

                // Pricing Summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F12))
                        .border(1.dp, LuxuryBorderGrey)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bag Subtotal", color = LuxuryTextGrey)
                            Text("₦${String.format("%,.0f", subtotal)}", color = LuxuryWhite, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Home Delivery Fee", color = LuxuryTextGrey)
                            if (user?.isLoggedIn == true) {
                                Text("₦${String.format("%,.0f", shippingFee)}", color = LuxuryWhite, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Calculated at Checkout", color = LuxuryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = LuxuryBorderGrey)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL AMOUNT", fontWeight = FontWeight.Black, color = LuxuryWhite, fontSize = 16.sp)
                            Text(
                                "₦${String.format("%,.0f", total)}",
                                color = LuxuryGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (user?.isLoggedIn == true) {
                                    viewModel.navigateTo(Screen.Checkout)
                                } else {
                                    viewModel.navigateTo(Screen.Register)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("checkout_btn")
                        ) {
                            Text(
                                if (user?.isLoggedIn == true) "PROCEED TO SECURE CHECKOUT" else "REGISTER TO COMPLETE ORDER",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        BrandingFooter()
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: JayTrendzViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
        border = BorderStroke(1.dp, LuxuryBorderGrey),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                imageSource = item.productImageDrawableName,
                contentDescription = item.productName,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.productName,
                    color = LuxuryWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "Size: ${item.selectedSize} • Color: ${item.selectedColor}",
                    color = LuxuryTextGrey,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "₦${String.format("%,.0f", item.productPrice)}",
                    color = LuxuryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Quantity controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.updateCartQuantity(item, item.quantity - 1) },
                    modifier = Modifier.size(32.dp).background(Color(0xFF1E1E24), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Less", tint = LuxuryWhite, modifier = Modifier.size(16.dp))
                }

                Text(
                    item.quantity.toString(),
                    color = LuxuryWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = { viewModel.updateCartQuantity(item, item.quantity + 1) },
                    modifier = Modifier.size(32.dp).background(Color(0xFF1E1E24), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "More", tint = LuxuryWhite, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CheckoutScreen(viewModel: JayTrendzViewModel) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartState.collectAsStateWithLifecycle()

    val subtotal = cartItems.sumOf { it.productPrice * it.quantity }
    val shippingFee = if (user != null) viewModel.calculateShippingFee(user!!.state) else 0.0
    val total = subtotal + shippingFee

    var paymentMethod by remember { mutableStateOf("Bank Transfer") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = "ORDER CHECKOUT",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "DELIVERY ADDRESS DETAILS",
                fontWeight = FontWeight.Black,
                color = LuxuryGold,
                fontSize = 14.sp
            )

            if (user != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryBorderGrey)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(user!!.fullName, fontWeight = FontWeight.Bold, color = LuxuryWhite, fontSize = 16.sp)
                        Text("Phone: ${user!!.phone}", color = LuxuryWhite, fontSize = 14.sp)
                        Text(
                            "Doorstep Address: Apt/House ${user!!.houseNumber}, ${user!!.streetAddress}, ${user!!.city}, ${user!!.state} State, Nigeria.",
                            color = LuxuryTextGrey,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "📍 Ship Origin: Dispatching direct to doorstep from our main Dispatch Hub.",
                            fontSize = 11.sp,
                            color = LuxuryGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(color = LuxuryBorderGrey)

            Text(
                "SELECT PAYMENT METHOD",
                fontWeight = FontWeight.Black,
                color = LuxuryGold,
                fontSize = 14.sp
            )

            // Payment Option Bank Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (paymentMethod == "Bank Transfer") Color(0xFF201B0E) else Color(0xFF131317))
                    .border(
                        1.dp,
                        if (paymentMethod == "Bank Transfer") LuxuryGold else LuxuryBorderGrey,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { paymentMethod = "Bank Transfer" }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (paymentMethod == "Bank Transfer"),
                    onClick = { paymentMethod = "Bank Transfer" },
                    colors = RadioButtonDefaults.colors(selectedColor = LuxuryGold)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Direct Bank Transfer", fontWeight = FontWeight.Bold, color = LuxuryWhite)
                    Text("Secure transfer with simulated verification", fontSize = 11.sp, color = LuxuryTextGrey)
                }
            }

            // Payment Option PIN Payment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (paymentMethod == "PIN Payment") Color(0xFF201B0E) else Color(0xFF131317))
                    .border(
                        1.dp,
                        if (paymentMethod == "PIN Payment") LuxuryGold else LuxuryBorderGrey,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { paymentMethod = "PIN Payment" }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (paymentMethod == "PIN Payment"),
                    onClick = { paymentMethod = "PIN Payment" },
                    colors = RadioButtonDefaults.colors(selectedColor = LuxuryGold)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Card PIN Payment", fontWeight = FontWeight.Bold, color = LuxuryWhite)
                    Text("Authorize securely using your 4-digit card PIN", fontSize = 11.sp, color = LuxuryTextGrey)
                }
            }

            Divider(color = LuxuryBorderGrey)

            // Checkout Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
                border = BorderStroke(1.dp, LuxuryBorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = LuxuryTextGrey)
                        Text("₦${String.format("%,.0f", subtotal)}", color = LuxuryWhite)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery straight to Doorstep", color = LuxuryTextGrey)
                        Text("₦${String.format("%,.0f", shippingFee)}", color = LuxuryWhite)
                    }
                    Divider(color = LuxuryBorderGrey)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GRAND TOTAL", fontWeight = FontWeight.Black, color = LuxuryWhite, fontSize = 15.sp)
                        Text("₦${String.format("%,.0f", total)}", fontWeight = FontWeight.Black, color = LuxuryGold, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.placeOrder(paymentMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_order_btn")
            ) {
                Text(
                    "PLACE ORDER & PAY ₦${String.format("%,.0f", total)}",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        BrandingFooter()
    }
}

@Composable
fun PaymentScreen(
    orderId: String,
    totalAmount: Double,
    shippingFee: Double,
    viewModel: JayTrendzViewModel
) {
    val orders by viewModel.ordersState.collectAsStateWithLifecycle()
    val order = orders.find { it.id == orderId }
    val paymentMethod = order?.paymentMethod ?: "Bank Transfer"

    // Bank Transfer inputs
    var senderBank by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }

    // PIN Payment inputs
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var cardPin by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = if (paymentMethod == "Bank Transfer") "BANK TRANSFER PAYMENT" else "CARD PIN PAYMENT",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Secure gateway badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                    .border(1.dp, LuxuryBorderGrey, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = "Secure", tint = LuxuryGold)
                Text("SECURE GATEWAY SIMULATION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxuryWhite)
            }

            Text(
                text = "PAYING JAY TRENDZ HUB",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LuxuryTextGrey
            )

            Text(
                text = "₦${String.format("%,.2f", totalAmount)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = LuxuryGold
            )

            Text(
                text = "Order ID: $orderId",
                fontSize = 14.sp,
                color = LuxuryWhite,
                fontWeight = FontWeight.Bold
            )

            Divider(color = LuxuryBorderGrey)

            if (paymentMethod == "Bank Transfer") {
                // Direct Bank account details show
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "OFFICIAL JAY TRENDZ BANK DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = LuxuryGold,
                            letterSpacing = 1.sp
                        )
                        Text("Bank: Zenith Bank PLC", color = LuxuryWhite, fontWeight = FontWeight.Bold)
                        Text("Account No: 1012345678", color = LuxuryWhite, fontWeight = FontWeight.Bold)
                        Text("Account Name: Jay Trendz Enterprise", color = LuxuryWhite)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Please transfer exactly ₦${String.format("%,.2f", totalAmount)} into this account, then enter your payment details below to complete your order.",
                            fontSize = 11.sp,
                            color = LuxuryTextGrey,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Bank Transfer Input Fields
                OutlinedTextField(
                    value = senderBank,
                    onValueChange = { senderBank = it },
                    label = { Text("Your Bank Name (e.g. GTBank, Access Bank)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedTextColor = LuxuryWhite,
                        unfocusedTextColor = LuxuryWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("sender_bank_input")
                )

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender's Account Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedTextColor = LuxuryWhite,
                        unfocusedTextColor = LuxuryWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("sender_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (senderBank.isBlank() || senderName.isBlank()) {
                            Toast.makeText(context, "Please fill in all details to verify your transfer", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.confirmPayment(orderId)
                        Toast.makeText(context, "Transfer Verification Started Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("verify_payment_btn")
                ) {
                    Text(
                        "I HAVE MADE THE TRANSFER",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

            } else {
                // Elegant Visual Credit Card representation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LuxuryGold),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131317))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Decorative card background lines
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "JAY TRENDZ LUXURY",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = LuxuryGold,
                                    letterSpacing = 1.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Card Chip",
                                    tint = LuxuryGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                text = if (cardNumber.isNotBlank()) cardNumber else "••••  ••••  ••••  ••••",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxuryWhite,
                                letterSpacing = 2.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("CARD HOLDER", fontSize = 8.sp, color = LuxuryTextGrey)
                                    Text(
                                        text = order?.fullName?.uppercase() ?: "VALUED CLIENT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxuryWhite
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("EXPIRY", fontSize = 8.sp, color = LuxuryTextGrey)
                                    Text(
                                        text = if (cardExpiry.isNotBlank()) cardExpiry else "MM/YY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxuryWhite
                                    )
                                }
                            }
                        }
                    }
                }

                // PIN Payment Input Fields
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Card Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedTextColor = LuxuryWhite,
                        unfocusedTextColor = LuxuryWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("payment_card_num")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cardExpiry,
                        onValueChange = { cardExpiry = it },
                        label = { Text("Expiry (MM/YY)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = LuxuryBorderGrey,
                            focusedTextColor = LuxuryWhite,
                            unfocusedTextColor = LuxuryWhite
                        ),
                        modifier = Modifier.weight(1f).testTag("payment_expiry")
                    )

                    OutlinedTextField(
                        value = cardCvv,
                        onValueChange = { cardCvv = it },
                        label = { Text("CVV") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = LuxuryBorderGrey,
                            focusedTextColor = LuxuryWhite,
                            unfocusedTextColor = LuxuryWhite
                        ),
                        modifier = Modifier.weight(1f).testTag("payment_cvv")
                    )
                }

                // 4-Digit Card PIN Input (Secure PIN)
                OutlinedTextField(
                    value = cardPin,
                    onValueChange = { if (it.length <= 4) cardPin = it },
                    label = { Text("Enter 4-Digit Card PIN") },
                    placeholder = { Text("••••") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedTextColor = LuxuryWhite,
                        unfocusedTextColor = LuxuryWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("payment_card_pin")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (cardNumber.isBlank() || cardExpiry.isBlank() || cardCvv.isBlank() || cardPin.length < 4) {
                            Toast.makeText(context, "Please fill in your card details & 4-digit PIN", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.confirmPayment(orderId)
                        Toast.makeText(context, "Payment Authorized & Confirmed!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("verify_payment_btn")
                ) {
                    Text(
                        "AUTHORIZE PIN & COMPLETE PAYMENT",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        BrandingFooter()
    }
}

@Composable
fun TrackerScreen(orderId: String, viewModel: JayTrendzViewModel) {
    val orders by viewModel.ordersState.collectAsStateWithLifecycle()
    val order = orders.find { it.id == orderId }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = "ORDER DESPATCH TRACKER",
            onBackClick = { viewModel.navigateTo(Screen.Catalog) }
        )

        if (order == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Retrieving dispatch data...", color = LuxuryTextGrey)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning note highlighting no pickup model
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1404)),
                    border = BorderStroke(1.dp, LuxuryGold)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HomeWork, contentDescription = "Home Delivery", tint = LuxuryGold, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "STRICT NO-PICKUP MODEL",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = LuxuryGold
                            )
                            Text(
                                "Your order is dispatched from our Main Hub and delivered directly to your doorstep. Sit back and relax!",
                                fontSize = 10.sp,
                                color = LuxuryWhite,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Text(
                    "ORDER: ${order.id}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = LuxuryWhite
                )

                Text(
                    "Current Location & Status:",
                    fontSize = 11.sp,
                    color = LuxuryTextGrey,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryBorderGrey)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = "Shipping", tint = LuxuryGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = order.status.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = LuxuryGold
                            )
                        }

                        Text(
                            text = "Recipient address: House ${order.houseNumber}, ${order.streetAddress}, ${order.city}, ${order.state} State, Nigeria.",
                            fontSize = 12.sp,
                            color = LuxuryWhite
                        )
                    }
                }

                // Custom Canvas Route Drawing from Ehime Mbano to Customer State
                Text(
                    "DISPATCH ROUTE VISUALIZATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = LuxuryGold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF0F0F12), RoundedCornerShape(12.dp))
                        .border(1.dp, LuxuryBorderGrey, RoundedCornerShape(12.dp))
                ) {
                    val progressValue = order.trackingProgress
                    val userState = order.state

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val startX = width * 0.2f
                        val startY = height * 0.7f
                        val endX = width * 0.8f
                        val endY = height * 0.3f

                        // Draw background state grids/markers
                        drawCircle(
                            color = Color(0xFFD4AF37).copy(alpha = 0.1f),
                            radius = 40f,
                            center = Offset(startX, startY)
                        )
                        drawCircle(
                            color = Color(0xFFD4AF37).copy(alpha = 0.1f),
                            radius = 40f,
                            center = Offset(endX, endY)
                        )

                        // Route Path
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(startX, startY)
                            quadraticTo(
                                (startX + endX) / 2, (startY + endY) / 2 - 40f,
                                endX, endY
                            )
                        }

                        // Draw path dotted
                        drawPath(
                            path = path,
                            color = LuxuryBorderGrey,
                            style = Stroke(
                                width = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )

                        // Draw actual transit line
                        val currentX = startX + (endX - startX) * progressValue
                        // Simple curve interpolation
                        val controlY = (startY + endY) / 2 - 40f
                        val t = progressValue
                        val currentY = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * controlY + t * t * endY

                        val completedPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(startX, startY)
                            quadraticTo(
                                (startX + endX) / 2, (startY + endY) / 2 - 40f,
                                currentX, currentY
                            )
                        }

                        drawPath(
                            path = completedPath,
                            color = LuxuryGold,
                            style = Stroke(width = 6f)
                        )

                        // Draw Dispatch Center (Ehime Mbano Hub)
                        drawCircle(
                            color = LuxuryGold,
                            radius = 8f,
                            center = Offset(startX, startY)
                        )

                        // Draw Destination
                        drawCircle(
                            color = Color.Green,
                            radius = 8f,
                            center = Offset(endX, endY)
                        )

                        // Label markers in Canvas
                        // (Text drawing is handled below in overlay text blocks)
                    }

                    // Text labels overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 12.dp)
                    ) {
                        Column {
                            Text("Main Dispatch Hub", color = LuxuryGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Imo State (Origin)", color = LuxuryTextGrey, fontSize = 8.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$userState State", color = Color.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Your Doorstep (Destination)", color = LuxuryTextGrey, fontSize = 8.sp)
                        }
                    }

                    // Animated Delivery Status overlay badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(LuxuryBlack.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                            .border(1.dp, LuxuryGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${(order.trackingProgress * 100).toInt()}% DELIVERED",
                            color = LuxuryWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Courier Details & Quick Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryBorderGrey)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "ASSIGNED DELIVERY COURIER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = LuxuryGold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF25252B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = "Courier", tint = LuxuryGold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Emeka Chinedu", fontWeight = FontWeight.Bold, color = LuxuryWhite)
                                    Text("Jay Trendz Dispatch Partner", fontSize = 11.sp, color = LuxuryTextGrey)
                                }
                            }

                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09042128365"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(LuxuryGold, CircleShape)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call Courier", tint = LuxuryBlack)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.navigateTo(Screen.Catalog) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131317), contentColor = LuxuryGold),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LuxuryBorderGrey),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("BACK TO HOME CATALOG", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }

        BrandingFooter()
    }
}

@Composable
fun SupportHubScreen(viewModel: JayTrendzViewModel) {
    val messages by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val isTyping = viewModel.isSupportTyping
    val context = LocalContext.current
    var chatInputText by remember { mutableStateOf("") }
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val negotiationProduct = viewModel.negotiationProduct
    val isOwner = user?.email == "onyedikachijeremiah60@gmail.com"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = if (isOwner) "CUSTOMER CHATS" else if (negotiationProduct != null) "PRODUCT CHAT" else "CHAT WITH OWNER",
            actions = {
                // Call support action
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09042128365"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1D1D22), CircleShape)
                        .border(1.dp, LuxuryBorderGrey, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = LuxuryGold
                    )
                }
            }
        )

        if (isOwner) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                border = BorderStroke(1.dp, LuxuryGold)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Admin Active",
                        tint = LuxuryGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OWNER MODE: Chatting directly with your customer. Replies will be signed as Onyedikachi (Owner).",
                        color = LuxuryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 15.sp
                    )
                }
            }
        } else if (negotiationProduct != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                border = BorderStroke(1.dp, LuxuryBorderGrey)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Product",
                            tint = LuxuryGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "INQUIRY: ${negotiationProduct.name}",
                                color = LuxuryWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Price: ₦${String.format("%,.0f", negotiationProduct.price)}",
                                color = LuxuryGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.clearNegotiation() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Split Layout: Quick Actions & Live Chat
        Column(modifier = Modifier.weight(1f)) {
            // FAQ and quick contact horizontal strips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F12))
                    .border(1.dp, LuxuryBorderGrey)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09042128365"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryBorderGrey),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp).testTag("call_support_btn")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = LuxuryGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("09042128365", color = LuxuryWhite, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Email Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:onyedikachijeremiah60@gmial.com"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryBorderGrey),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.5f).height(44.dp).testTag("email_support_btn")
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Email", tint = LuxuryGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("onyedikachijeremiah60@gmial.com", color = LuxuryWhite, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }

            // Chat Messages Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    // Chat Onboarding/Greeting
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.jay_trendz_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(1.dp, LuxuryGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "LIVE IN-APP HELP CHAT",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = LuxuryGold
                        )
                        Text(
                            "Ask us anything regarding sizing, shipping, product stock, or delivery status.",
                            color = LuxuryTextGrey,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Prompts
                        val prompts = listOf(
                            "How is shipping calculated?",
                            "Do you have sizes for Agbada?",
                            "Where is the dispatch origin?",
                            "How to make bank transfers?"
                        )

                        prompts.forEach { pText ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.sendChatMessage(pText) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                                border = BorderStroke(1.dp, LuxuryBorderGrey)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(pText, color = LuxuryWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Send", tint = LuxuryGold, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubble(message)
                        }

                        if (isTyping) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .background(Color(0xFF131317), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        "Support bot is drafting reply...",
                                        color = LuxuryGold,
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Chat input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F12))
                    .border(1.dp, LuxuryBorderGrey)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { 
                        val hint = if (isOwner) "Type reply as Owner..." else if (negotiationProduct != null) "Ask the Owner about this item..." else "Ask the Owner..."
                        Text(hint, color = LuxuryTextGrey) 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = LuxuryBorderGrey,
                        focusedTextColor = LuxuryWhite,
                        unfocusedTextColor = LuxuryWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("chat_input_text")
                )

                IconButton(
                    onClick = {
                        if (chatInputText.isNotBlank()) {
                            viewModel.sendChatMessage(chatInputText)
                            chatInputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .background(LuxuryGold, CircleShape)
                        .testTag("chat_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = LuxuryBlack
                    )
                }
            }
        }

        BrandingFooter()
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "User"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 12.dp
                        )
                    )
                    .background(if (isUser) LuxuryGold else Color(0xFF131317))
                    .border(
                        1.dp,
                        if (isUser) LuxuryGold else LuxuryBorderGrey,
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 12.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (isUser) LuxuryBlack else LuxuryWhite,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isUser) "You" else "Jay Trendz Bot",
                fontSize = 9.sp,
                color = LuxuryTextGrey,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileScreen(viewModel: JayTrendzViewModel) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val orders by viewModel.ordersState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = "YOUR PROFILE"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (user == null || !user!!.isLoggedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryBorderGrey)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("NOT REGISTERED", fontWeight = FontWeight.Bold, color = LuxuryWhite)
                        Text(
                            "Create an account to configure doorstep delivery address and see order history.",
                            color = LuxuryTextGrey,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Register) },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CREATE ACCOUNT", fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = {
                                viewModel.registerUser(
                                    fullName = "Onyedikachi Jeremiah",
                                    email = "onyedikachijeremiah60@gmail.com",
                                    phone = "09042128365",
                                    street = "Umualuwaka in Umueze 1",
                                    house = "",
                                    city = "Ehime Mbano",
                                    state = "Imo"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B1F), contentColor = LuxuryGold),
                            border = BorderStroke(1.dp, LuxuryGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Admin", tint = LuxuryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LOG IN AS ADMIN / OWNER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Profile Information card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                    border = BorderStroke(1.dp, LuxuryBorderGrey)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(LuxuryGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user!!.fullName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    color = LuxuryBlack,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(user!!.fullName, fontWeight = FontWeight.Black, color = LuxuryWhite, fontSize = 16.sp)
                                Text(user!!.email, color = LuxuryTextGrey, fontSize = 12.sp)
                            }
                        }

                        Divider(color = LuxuryBorderGrey, modifier = Modifier.padding(vertical = 8.dp))

                        Text("DELIVERY COORDINATION INFO", fontWeight = FontWeight.Bold, color = LuxuryGold, fontSize = 12.sp)
                        Text("Phone: ${user!!.phone}", color = LuxuryWhite, fontSize = 13.sp)
                        Text(
                            "Address: House ${user!!.houseNumber}, ${user!!.streetAddress}, ${user!!.city}, ${user!!.state} State, Nigeria.",
                            color = LuxuryWhite,
                            fontSize = 13.sp
                        )

                        if (user?.email == "onyedikachijeremiah60@gmail.com") {
                            Divider(color = LuxuryBorderGrey, modifier = Modifier.padding(vertical = 8.dp))

                            Text("ADMIN & OWNER SUITE", fontWeight = FontWeight.Bold, color = LuxuryGold, fontSize = 12.sp)
                            Button(
                                onClick = { viewModel.navigateTo(Screen.UploadProduct) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("POST NEW CLOTHES & ACCESSORIES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261D1E), contentColor = Color.Red),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("LOG OUT PROFILE", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Customer Order list
                Text(
                    "YOUR ORDERS (${orders.size})",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = LuxuryGold,
                    letterSpacing = 1.sp
                )

                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No orders placed yet.", color = LuxuryTextGrey)
                    }
                } else {
                    orders.forEach { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.Tracker(order.id)) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                            border = BorderStroke(1.dp, LuxuryBorderGrey)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Order #${order.id}", fontWeight = FontWeight.Bold, color = LuxuryWhite)
                                    Text(
                                        order.status,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxuryGold,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Paid: ${order.paymentStatus}", color = LuxuryTextGrey, fontSize = 12.sp)
                                    Text("Total: ₦${String.format("%,.0f", order.totalAmount)}", color = LuxuryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Map", tint = LuxuryGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tap to track doorstep progress", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        BrandingFooter()
    }
}

@Composable
fun UploadProductScreen(viewModel: JayTrendzViewModel) {
    val context = LocalContext.current
    val user by viewModel.userState.collectAsStateWithLifecycle()

    if (user == null || user?.email != "onyedikachijeremiah60@gmail.com") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LuxuryBlack)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Access Denied",
                    tint = Color.Red,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ACCESS DENIED",
                    color = Color.Red,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Only the Owner & Admin of Jay Trendz (Onyedikachi Jeremiah) is authorized to post or manage fashion items.",
                    color = LuxuryWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.navigateBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GO BACK TO BOUTIQUE", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Men's Wear") }
    var sizes by remember { mutableStateOf("S, M, L, XL, XXL") }
    var colors by remember { mutableStateOf("Onyx Black, Royal Gold, Ivory White") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val categories = listOf("Men's Wear", "Women's Wear", "Accessories", "New Arrivals")

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        LuxuryHeader(
            title = "MERCHANT PORTAL",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Section Header
            Text(
                text = "UPLOAD NEW APPAREL / ACCESSORY",
                color = LuxuryGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            // Select Image Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { galleryLauncher.launch("image/*") }
                    .testTag("upload_image_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                border = BorderStroke(1.dp, if (selectedImageUri != null) LuxuryGold else LuxuryBorderGrey),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        // Display selected image preview
                        ProductImage(
                            imageSource = selectedImageUri.toString(),
                            contentDescription = "Selected Product Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Overlay with change option
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload", tint = LuxuryGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CHANGE IMAGE", color = LuxuryWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Place holder for selecting image
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Image Icon",
                                tint = LuxuryGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "TAP TO SELECT FASHION IMAGE",
                                color = LuxuryWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            Text(
                                "Supports JPEGs, PNGs from Gallery",
                                color = LuxuryTextGrey,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Input fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product / Item Name", color = LuxuryTextGrey) },
                placeholder = { Text("e.g. Signature Gold-Laced Boubou", color = LuxuryTextGrey) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedTextColor = LuxuryWhite,
                    unfocusedTextColor = LuxuryWhite
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("upload_name_input")
            )

            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Price (₦ - Naira)", color = LuxuryTextGrey) },
                placeholder = { Text("e.g. 35000", color = LuxuryTextGrey) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedTextColor = LuxuryWhite,
                    unfocusedTextColor = LuxuryWhite
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("upload_price_input")
            )

            // Select Category Selection Row
            Text(
                text = "SELECT CATEGORY",
                color = LuxuryTextGrey,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Let's make it wrapping or Scrollable Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) LuxuryGold else Color(0xFF131317))
                                .border(
                                    1.dp,
                                    if (isSelected) LuxuryGold else LuxuryBorderGrey,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.uppercase(),
                                color = if (isSelected) LuxuryBlack else LuxuryWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Product Description", color = LuxuryTextGrey) },
                placeholder = { Text("Tell your customers about the material, embroidery, fit, origin, etc...", color = LuxuryTextGrey) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedTextColor = LuxuryWhite,
                    unfocusedTextColor = LuxuryWhite
                ),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("upload_description_input")
            )

            OutlinedTextField(
                value = sizes,
                onValueChange = { sizes = it },
                label = { Text("Available Sizes (Comma separated)", color = LuxuryTextGrey) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedTextColor = LuxuryWhite,
                    unfocusedTextColor = LuxuryWhite
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("upload_sizes_input")
            )

            OutlinedTextField(
                value = colors,
                onValueChange = { colors = it },
                label = { Text("Available Colors (Comma separated)", color = LuxuryTextGrey) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = LuxuryBorderGrey,
                    focusedTextColor = LuxuryWhite,
                    unfocusedTextColor = LuxuryWhite
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("upload_colors_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Upload Button
            Button(
                onClick = {
                    val priceValue = priceStr.toDoubleOrNull()
                    if (name.isBlank()) {
                        Toast.makeText(context, "Please enter item name", Toast.LENGTH_SHORT).show()
                    } else if (priceValue == null || priceValue <= 0.0) {
                        Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    } else if (selectedImageUri == null) {
                        Toast.makeText(context, "Please select an image of the item", Toast.LENGTH_SHORT).show()
                    } else {
                        // Persist the image to local storage
                        val storedPath = saveUriToInternalStorage(context, selectedImageUri!!)
                        if (storedPath != null) {
                            val newProduct = Product(
                                id = "CUSTOM_${System.currentTimeMillis()}",
                                name = name,
                                price = priceValue,
                                category = selectedCategory,
                                imageDrawableName = storedPath,
                                description = description.ifBlank { "No description available." },
                                sizes = sizes.ifBlank { "Free Size" },
                                colors = colors.ifBlank { "As Shown" },
                                inStock = true
                            )
                            viewModel.addProduct(newProduct) {
                                Toast.makeText(context, "🎉 Item uploaded successfully to $selectedCategory!", Toast.LENGTH_LONG).show()
                                viewModel.navigateBack()
                            }
                        } else {
                            Toast.makeText(context, "Failed to store image local copy. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = LuxuryBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("upload_submit_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Add Item")
                Spacer(modifier = Modifier.width(8.dp))
                Text("PUBLISH FASHION ITEM", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}
