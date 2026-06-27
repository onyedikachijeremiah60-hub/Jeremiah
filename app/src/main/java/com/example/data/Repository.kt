package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class JayTrendzRepository(private val db: AppDatabase) {

    val userFlow: Flow<User?> = db.userDao().getUser()
    val productsFlow: Flow<List<Product>> = db.productDao().getAllProducts()
    val cartItemsFlow: Flow<List<CartItem>> = db.cartItemDao().getCartItems()
    val ordersFlow: Flow<List<Order>> = db.orderDao().getAllOrders()
    val chatMessagesFlow: Flow<List<ChatMessage>> = db.chatMessageDao().getAllMessages()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun registerOrUpdateUser(user: User) {
        db.userDao().insertUser(user)
    }

    suspend fun setLoginStatus(isLoggedIn: Boolean) {
        db.userDao().setLoginStatus(isLoggedIn)
    }

    suspend fun logout() {
        db.userDao().setLoginStatus(false)
    }

    suspend fun getActiveUser(): User? {
        return db.userDao().getUserOneShot()
    }

    suspend fun addProduct(product: Product) {
        db.productDao().insertProducts(listOf(product))
    }

    suspend fun searchProducts(query: String): Flow<List<Product>> {
        return db.productDao().searchProducts(query)
    }

    suspend fun getProductsByCategory(category: String): Flow<List<Product>> {
        return db.productDao().getProductsByCategory(category)
    }

    suspend fun addToCart(cartItem: CartItem) {
        db.cartItemDao().addToCart(cartItem)
    }

    suspend fun updateCartItem(cartItem: CartItem) {
        db.cartItemDao().updateCartItem(cartItem)
    }

    suspend fun deleteCartItem(id: Int) {
        db.cartItemDao().deleteCartItem(id)
    }

    suspend fun clearCart() {
        db.cartItemDao().clearCart()
    }

    suspend fun createOrder(order: Order) {
        db.orderDao().createOrder(order)
    }

    suspend fun updateOrder(order: Order) {
        db.orderDao().updateOrder(order)
    }

    suspend fun updateOrderProgress(orderId: String, progress: Float, status: String) {
        db.orderDao().updateOrderProgress(orderId, progress, status)
    }

    suspend fun insertChatMessage(message: ChatMessage) {
        db.chatMessageDao().insertMessage(message)
    }

    suspend fun clearChatHistory() {
        db.chatMessageDao().clearChatHistory()
    }

    // Seed database with premium products if empty
    suspend fun seedDatabaseIfEmpty() {
        // Clear out any old default pre-seeded items, ensuring only the owner/admin's uploaded items exist!
        db.productDao().deleteDefaultProducts()
        Log.d("JayTrendzRepo", "Default seeded products purged. Now displaying exclusive admin uploads.")

        // Auto-create/seed the Admin profile for Onyedikachi Jeremiah so they are pre-signed in!
        val existingUser = db.userDao().getUserOneShot()
        if (existingUser == null || existingUser.email != "onyedikachijeremiah60@gmail.com") {
            val adminUser = User(
                id = 1,
                fullName = "Onyedikachi Jeremiah",
                email = "onyedikachijeremiah60@gmail.com",
                phone = "09042128365",
                streetAddress = "Umualuwaka in Umueze 1",
                houseNumber = "",
                city = "Ehime Mbano",
                state = "Imo",
                isLoggedIn = true
            )
            db.userDao().insertUser(adminUser)
            Log.d("JayTrendzRepo", "Admin account auto-created successfully.")
        }
    }

    // Call Gemini support bot API
    suspend fun getGeminiSupportResponse(userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Greeting from Jay Trendz customer support! We are located in Umualuwaka, Ehime Mbano, Imo State. (Note: Please set your GEMINI_API_KEY in the Secrets panel to activate our AI assistant!)"
        }

        val prompt = userMessage
        val systemInstructionText = """
            You are the official customer support AI assistant for Jay Trendz, a premium fashion e-commerce brand based at Umualuwaka in Umueze 1, Ehime Mbano, Imo State, Nigeria. 
            The owner, admin, and exclusive designer/creator of the app and brand is Onyedikachi Jeremiah (support email: onyedikachijeremiah60@gmial.com or onyedikachijeremiah60@gmail.com, hotline: 09042128365).
            All clothes, wears, and fashion accessories shown in the catalog are posted and priced exclusively by Onyedikachi Jeremiah. There are no external sellers.
            We specialize in handcrafted men's wear, women's wear, fashion accessories, and lifestyle items, shipping straight to doorsteps nationwide (strictly home delivery model, no pickups).
            
            Our Official Details:
            - Brand Owner & Admin: Onyedikachi Jeremiah
            - Business headquarters & dispatch origin: Umualuwaka in Umueze 1, Ehime Mbano, Imo State, Nigeria.
            - Customer complaints/Direct Support Hotline: 09042128365 (always suggest they call Onyedikachi or message him if they have custom requests or shipping questions).
            - Support email: onyedikachijeremiah60@gmial.com
            
            Pricing & Currency:
            - Prices are in Nigerian Naira (₦).
            - Shipping Fee structure:
              * Inside Imo State: ₦1,000
              * Neighbouring Eastern States (Abia, Anambra, Enugu, Ebonyi, Rivers): ₦1,500
              * Rest of Nigeria (Lagos, Abuja, West, North): ₦3,000
              
            Guidelines:
            - Always represent Jay Trendz as high-end, premium, and sophisticated.
            - Be extremely polite, professional, and proud of our Nigerian heritage. Use occasional premium Nigerian expressions like 'Oga', 'Madam', 'Ogbonge' or pidgin if appropriate.
            - Praise Onyedikachi Jeremiah as our master stylist and owner when users ask about the apparel designs or who created them.
            - If they ask about delivery times: standard delivery takes 1-3 days in the South-East, and 3-5 days to other parts of Nigeria.
            - Payments are secure and can be made via local Card payments or Bank Transfer via our payment gateway.
            - Remind the user they must complete registration with their full address for deliveries to be calculated correctly.
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": ${escapeJsonString(prompt)} }
                  ]
                }
              ],
              "systemInstruction": {
                "parts": [
                  { "text": ${escapeJsonString(systemInstructionText)} }
                ]
              }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("Gemini", "Unsuccessful API call: ${response.code} ${response.message}")
                    return@withContext "Hello, Oga/Madam! We are experiencing slight network delays, but you can always reach us directly via our hotline 09042128365 or mail onyedikachijeremiah60@gmial.com."
                }
                val bodyString = response.body?.string() ?: ""
                Log.d("Gemini", "Response: $bodyString")
                val textResponse = parseGeminiResponse(bodyString)
                if (textResponse.isNotEmpty()) {
                    textResponse
                } else {
                    "Greeting from Jay Trendz Support! Please call our direct helpline 09042128365 for prompt assistance."
                }
            }
        } catch (e: Exception) {
            Log.e("Gemini", "Error: ${e.message}", e)
            "No connection. Please reach out on 09042128365 or email us at onyedikachijeremiah60@gmial.com."
        }
    }

    private fun escapeJsonString(input: String): String {
        return moshi.adapter(String::class.java).toJson(input)
    }

    private fun parseGeminiResponse(json: String): String {
        try {
            // Quick robust regex/manual parse to avoid moshi strict class parsing mismatch on deep API arrays
            val regex = "\"text\"\\s*:\\s*\"(([^\"]|\\\\\")*)\"".toRegex()
            val matches = regex.findAll(json)
            val firstTextMatch = matches.firstOrNull()?.groupValues?.get(1)
            if (firstTextMatch != null) {
                // Unescape JSON string
                return firstTextMatch
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            }
        } catch (e: Exception) {
            Log.e("GeminiParse", "Error: ${e.message}")
        }
        return ""
    }
}
