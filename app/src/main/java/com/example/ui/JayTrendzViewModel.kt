package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Register : Screen()
    object Catalog : Screen()
    data class ProductDetails(val product: Product) : Screen()
    object Cart : Screen()
    object Checkout : Screen()
    data class Payment(val orderId: String, val totalAmount: Double, val shippingFee: Double) : Screen()
    data class Tracker(val orderId: String) : Screen()
    object SupportHub : Screen()
    object Profile : Screen()
    object UploadProduct : Screen()
}

class JayTrendzViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JayTrendzRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = JayTrendzRepository(database)
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Navigation State
    var currentScreen by mutableStateOf<Screen>(Screen.Catalog)
        private set

    private val _screenHistory = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        _screenHistory.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (_screenHistory.isNotEmpty()) {
            currentScreen = _screenHistory.removeAt(_screenHistory.size - 1)
        } else {
            currentScreen = Screen.Catalog
        }
    }

    // Active User State
    val userState: StateFlow<User?> = repository.userFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Products Catalog State
    val productsState: StateFlow<List<Product>> = repository.productsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Cart Items State
    val cartState: StateFlow<List<CartItem>> = repository.cartItemsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Orders State
    val ordersState: StateFlow<List<Order>> = repository.ordersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Support Chat Messages State
    val chatMessagesState: StateFlow<List<ChatMessage>> = repository.chatMessagesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI Search & Filter States
    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("All")
    var selectedSizeFilter by mutableStateOf("All")
    var selectedColorFilter by mutableStateOf("All")
    var maxPriceFilter by mutableStateOf(60000.0)
    var negotiationProduct by mutableStateOf<Product?>(null)

    fun startNegotiation(product: Product) {
        negotiationProduct = product
        navigateTo(Screen.SupportHub)
    }

    fun clearNegotiation() {
        negotiationProduct = null
    }

    // Chat Typing State
    var isSupportTyping by mutableStateOf(false)
        private set

    // Filtered Products Flow
    val filteredProducts: Flow<List<Product>> = snapshotFlow {
        Triple(searchQuery, selectedCategory, maxPriceFilter)
    }.combine(productsState) { (query, category, price), products ->
        products.filter { product ->
            val matchesSearch = query.isEmpty() || product.name.contains(query, ignoreCase = true) || 
                                product.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || product.category == category
            val matchesPrice = product.price <= price
            matchesSearch && matchesCategory && matchesPrice
        }
    }

    // User Operations
    fun registerUser(fullName: String, email: String, phone: String, street: String, house: String, city: String, state: String) {
        viewModelScope.launch {
            val user = User(
                fullName = fullName,
                email = email,
                phone = phone,
                streetAddress = street,
                houseNumber = house,
                city = city,
                state = state,
                isLoggedIn = true
            )
            repository.registerOrUpdateUser(user)
            navigateTo(Screen.Catalog)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            navigateTo(Screen.Catalog)
        }
    }

    // Cart Operations
    fun addToCart(product: Product, size: String, color: String) {
        viewModelScope.launch {
            val cartItem = CartItem(
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = 1,
                selectedSize = size,
                selectedColor = color,
                productImageDrawableName = product.imageDrawableName
            )
            repository.addToCart(cartItem)
        }
    }

    fun updateCartQuantity(cartItem: CartItem, newQuantity: Int) {
        viewModelScope.launch {
            if (newQuantity <= 0) {
                repository.deleteCartItem(cartItem.id)
            } else {
                repository.updateCartItem(cartItem.copy(quantity = newQuantity))
            }
        }
    }

    fun removeFromCart(id: Int) {
        viewModelScope.launch {
            repository.deleteCartItem(id)
        }
    }

    // Shipping Fee Calculation
    fun calculateShippingFee(state: String): Double {
        val normalizedState = state.trim().lowercase()
        return when {
            normalizedState == "imo" -> 1000.0
            normalizedState in listOf("abia", "anambra", "enugu", "ebonyi", "rivers") -> 1500.0
            else -> 3000.0
        }
    }

    // Place Order & Start Checkout
    fun placeOrder(paymentMethod: String) {
        val user = userState.value
        if (user == null || !user.isLoggedIn) {
            navigateTo(Screen.Register)
            return
        }

        viewModelScope.launch {
            val cartItems = cartState.value
            if (cartItems.isEmpty()) return@launch

            val subtotal = cartItems.sumOf { it.productPrice * it.quantity }
            val shippingFee = calculateShippingFee(user.state)
            val totalAmount = subtotal + shippingFee
            val orderId = "JT-${System.currentTimeMillis() % 1000000}"

            val order = Order(
                id = orderId,
                fullName = user.fullName,
                email = user.email,
                phone = user.phone,
                streetAddress = user.streetAddress,
                houseNumber = user.houseNumber,
                city = user.city,
                state = user.state,
                totalAmount = totalAmount,
                shippingFee = shippingFee,
                status = "Pending Payment",
                paymentMethod = paymentMethod,
                paymentStatus = "Pending"
            )

            repository.createOrder(order)
            navigateTo(Screen.Payment(orderId, totalAmount, shippingFee))
        }
    }

    // Complete Payment & Trigger Live Dispatch Tracking Simulation
    fun confirmPayment(orderId: String) {
        viewModelScope.launch {
            // Find order and update to Paid
            val orderList = dbQueryOrders()
            val order = orderList.find { it.id == orderId } ?: return@launch

            val paidOrder = order.copy(
                status = "Dispatching from Hub",
                paymentStatus = "Paid",
                trackingProgress = 0.05f
            )
            repository.updateOrder(paidOrder)
            repository.clearCart()

            // Navigate to tracking
            navigateTo(Screen.Tracker(orderId))

            // Start simulated real-time tracking updates
            simulateCourierTransit(orderId)
        }
    }

    private suspend fun dbQueryOrders(): List<Order> {
        return repository.ordersFlow.firstOrNull() ?: emptyList()
    }

    private fun simulateCourierTransit(orderId: String) {
        viewModelScope.launch {
            // Step 1: Dispatch
            delay(10000)
            updateOrderProgressLocal(orderId, 0.25f, "Courier leaving main dispatch hub, Imo State")

            // Step 2: Sorting at Hub
            delay(10000)
            updateOrderProgressLocal(orderId, 0.50f, "Sorting at regional transit facility")

            // Step 3: Out for doorstep delivery
            delay(10000)
            updateOrderProgressLocal(orderId, 0.80f, "Out for home delivery. Courier contacting customer")

            // Step 4: Arrived
            delay(10000)
            updateOrderProgressLocal(orderId, 1.0f, "Delivered to your doorstep!")
        }
    }

    private suspend fun updateOrderProgressLocal(orderId: String, progress: Float, status: String) {
        repository.updateOrderProgress(orderId, progress, status)
    }

    // Support Chat Logic
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val currentUser = userState.value
            val isOwner = currentUser?.email == "onyedikachijeremiah60@gmail.com"

            if (isOwner) {
                // The Owner is replying directly to the customer. Save as "Support" (Owner) so the customer sees it.
                val ownerMsg = ChatMessage(sender = "Support", message = messageText)
                repository.insertChatMessage(ownerMsg)
            } else {
                // A customer is sending a message or bargaining
                val userMsg = ChatMessage(sender = "User", message = messageText)
                repository.insertChatMessage(userMsg)

                isSupportTyping = true
                delay(1200) // Realistic typing delay

                // Add rich context if there is an active item being negotiated
                val activeProduct = negotiationProduct
                val systemContext = if (activeProduct != null) {
                    "The customer (named ${currentUser?.fullName ?: "Customer"}) is actively chatting with you directly " +
                    "about the product '${activeProduct.name}' (listed at ₦${activeProduct.price}) to ask questions or discuss buying. " +
                    "Please respond politely, warmly, and professionally as Onyedikachi Jeremiah, the master designer and owner of Jay Trendz. " +
                    "We are open to reasonable negotiations or custom size requests to make the sale. " +
                    "Sign off as 'Onyedikachi (Owner)'."
                } else {
                    "The customer is asking support questions. Respond warmly and professionally as Onyedikachi Jeremiah, the brand owner of Jay Trendz."
                }

                val reply = repository.getGeminiSupportResponse("$systemContext\n\nCustomer said: \"$messageText\"")
                val supportMsg = ChatMessage(sender = "Support", message = reply)
                repository.insertChatMessage(supportMsg)
                isSupportTyping = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun addProduct(product: Product, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addProduct(product)
            onSuccess()
        }
    }
}
