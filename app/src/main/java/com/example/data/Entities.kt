package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1, // Only one user profile saved locally
    val fullName: String,
    val email: String,
    val phone: String,
    val streetAddress: String,
    val houseNumber: String,
    val city: String,
    val state: String,
    val isLoggedIn: Boolean = false
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val category: String, // "Men's Wear", "Women's Wear", "Accessories", "New Arrivals"
    val imageDrawableName: String, // e.g. "mens_wear"
    val description: String,
    val sizes: String, // e.g. "M, L, XL, XXL"
    val colors: String, // e.g. "Black, White, Gold"
    val inStock: Boolean = true
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: String,
    val productName: String,
    val productPrice: Double,
    val quantity: Int,
    val selectedSize: String,
    val selectedColor: String,
    val productImageDrawableName: String
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val id: String, // e.g. JT-2026-0001
    val dateMillis: Long = System.currentTimeMillis(),
    val fullName: String,
    val email: String,
    val phone: String,
    val streetAddress: String,
    val houseNumber: String,
    val city: String,
    val state: String,
    val totalAmount: Double,
    val shippingFee: Double,
    val status: String, // "Processing", "Dispatching from Ehime Mbano", "In Transit", "Out for Delivery", "Delivered"
    val paymentMethod: String, // "Card Payment" or "Bank Transfer"
    val paymentStatus: String, // "Paid" or "Pending"
    val trackingProgress: Float = 0.0f // 0.0 to 1.0 representing distance from Ehime Mbano, Imo State to customer address
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "User" or "Support"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
