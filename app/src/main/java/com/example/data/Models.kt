package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // email or phone
    val phone: String,
    val email: String,
    val fullName: String,
    val role: String, // "Tenant", "Landlord", "Admin"
    val avatarUrl: String,
    val isVerified: Boolean = false,
    val idNumber: String = "", // BVN or NIN
    val rating: Float = 5.0f,
    val raterCount: Int = 1
)

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val landlordId: String,
    val title: String,
    val description: String,
    val pricePerYear: Double,
    val address: String,
    val country: String = "Nigeria",
    val state: String = "Borno State",
    val lga: String = "Gwoza",
    val city: String = "Gwoza Town",
    val street: String,
    val rooms: Int,
    val propertyType: String, // "Apartment", "House", "Bungalow", "Self-Contain"
    val amenities: String, // Comma-delimited list: e.g. "Water Running, Fenced, Solar Inverter"
    val imageUrl: String, // Dynamic or local resource description
    val latitude: Double, // For mocked Map view pinpointing Gwoza area
    val longitude: Double,
    val status: String = "Available" // "Available", "Rented", "Unavailable"
)

@Entity(tableName = "rental_requests")
data class RentalRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val tenantId: String,
    val landlordId: String,
    val durationYears: Int, // 1, 2, or 3 years
    val paymentInterval: String, // "Annually" or "Monthly"
    val status: String = "Pending", // "Pending", "Accepted", "Rejected", "Paid"
    val agreementSigned: Boolean = false,
    val agreementText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val senderId: String,
    val receiverId: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_receipts")
data class PaymentReceipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rentalRequestId: Long,
    val propertyId: Long,
    val tenantId: String,
    val amount: Double,
    val paymentGateway: String, // "Paystack" or "Flutterwave"
    val receiptNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val reviewerId: String,
    val revieweeId: String, // can be landlord or tenant
    val rating: Float,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)
