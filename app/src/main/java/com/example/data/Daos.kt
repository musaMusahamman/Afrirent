package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
}

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties")
    fun getAllProperties(): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: Long): Property?

    @Query("SELECT * FROM properties WHERE landlordId = :landlordId")
    fun getPropertiesByLandlord(landlordId: String): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE status = :status")
    fun getPropertiesByStatus(status: String): Flow<List<Property>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: Property)

    @Update
    suspend fun updateProperty(property: Property)

    @Delete
    suspend fun deleteProperty(property: Property)
}

@Dao
interface RentalRequestDao {
    @Query("SELECT * FROM rental_requests")
    fun getAllRentalRequests(): Flow<List<RentalRequest>>

    @Query("SELECT * FROM rental_requests WHERE tenantId = :tenantId")
    fun getRentalRequestsByTenant(tenantId: String): Flow<List<RentalRequest>>

    @Query("SELECT * FROM rental_requests WHERE landlordId = :landlordId")
    fun getRentalRequestsByLandlord(landlordId: String): Flow<List<RentalRequest>>

    @Query("SELECT * FROM rental_requests WHERE propertyId = :propertyId")
    fun getRentalRequestsByProperty(propertyId: Long): Flow<List<RentalRequest>>

    @Query("SELECT * FROM rental_requests WHERE id = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: Long): RentalRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RentalRequest): Long

    @Update
    suspend fun updateRequest(request: RentalRequest)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE propertyId = :propertyId ORDER BY timestamp ASC")
    fun getChatMessagesForProperty(propertyId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_receipts WHERE tenantId = :tenantId ORDER BY timestamp DESC")
    fun getReceiptsByTenant(tenantId: String): Flow<List<PaymentReceipt>>

    @Query("SELECT * FROM payment_receipts WHERE rentalRequestId = :requestId")
    fun getReceiptsByRequest(requestId: Long): Flow<List<PaymentReceipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: PaymentReceipt)

    @Query("SELECT * FROM payment_receipts")
    fun getAllReceipts(): Flow<List<PaymentReceipt>>
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE propertyId = :propertyId ORDER BY timestamp DESC")
    fun getReviewsForProperty(propertyId: Long): Flow<List<Review>>

    @Query("SELECT * FROM reviews WHERE revieweeId = :revieweeId ORDER BY timestamp DESC")
    fun getReviewsForUser(revieweeId: String): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)
}
