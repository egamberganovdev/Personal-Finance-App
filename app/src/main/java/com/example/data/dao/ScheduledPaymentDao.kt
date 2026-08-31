package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ScheduledPaymentEntity
import com.example.data.entity.ScheduledPaymentWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledPaymentDao {
    @Transaction
    @Query("SELECT * FROM scheduled_payments ORDER BY nextPaymentDateMillis ASC")
    fun getAllScheduledPaymentsWithDetails(): Flow<List<ScheduledPaymentWithDetails>>

    @Transaction
    @Query("SELECT * FROM scheduled_payments WHERE accountId = :accountId ORDER BY nextPaymentDateMillis ASC")
    fun getScheduledPaymentsByAccountWithDetails(accountId: Long): Flow<List<ScheduledPaymentWithDetails>>

    @Transaction
    @Query("SELECT * FROM scheduled_payments WHERE isActive = 1 ORDER BY nextPaymentDateMillis ASC")
    fun getActiveScheduledPaymentsWithDetails(): Flow<List<ScheduledPaymentWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPayment(scheduledPayment: ScheduledPaymentEntity): Long

    @Update
    suspend fun updateScheduledPayment(scheduledPayment: ScheduledPaymentEntity)

    @Delete
    suspend fun deleteScheduledPayment(scheduledPayment: ScheduledPaymentEntity)

    @Query("DELETE FROM scheduled_payments WHERE id = :id")
    suspend fun deleteScheduledPaymentById(id: Long)

    @Query("UPDATE scheduled_payments SET isActive = :isActive WHERE id = :id")
    suspend fun toggleActive(id: Long, isActive: Boolean)
}
