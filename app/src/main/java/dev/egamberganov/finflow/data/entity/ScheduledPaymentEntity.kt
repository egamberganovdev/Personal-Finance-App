package dev.egamberganov.finflow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_payments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("nextPaymentDateMillis")
    ]
)
data class ScheduledPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Long,
    val currency: String,
    val categoryId: Long,
    val accountId: Long,
    val frequency: String = "MONTHLY", // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextPaymentDateMillis: Long,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
