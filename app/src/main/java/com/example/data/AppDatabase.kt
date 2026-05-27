package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Property::class,
        RentalRequest::class,
        ChatMessage::class,
        PaymentReceipt::class,
        Review::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun rentalRequestDao(): RentalRequestDao
    abstract fun chatDao(): ChatDao
    abstract fun paymentDao(): PaymentDao
    abstract fun reviewDao(): ReviewDao
}
