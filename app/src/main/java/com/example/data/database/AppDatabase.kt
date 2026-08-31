package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AccountDao
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.ScheduledPaymentDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.AccountEntity
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ScheduledPaymentEntity
import com.example.data.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        ScheduledPaymentEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun scheduledPaymentDao(): ScheduledPaymentDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_finance.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
