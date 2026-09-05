package dev.egamberganov.finflow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
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
        Index("toAccountId"),
        Index("categoryId"),
        Index("dateMillis")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "INCOME", "EXPENSE", or "TRANSFER"
    val amount: Long, // Positive amount in units (source amount)
    val currency: String,
    val categoryId: Long? = null,
    val accountId: Long, // Source account
    val toAccountId: Long? = null, // Destination account for TRANSFER
    val exchangeRate: Double? = null, // For cross-currency transfer (1 source = X destination)
    val convertedAmount: Long? = null, // Destination amount for cross-currency transfer
    val dateMillis: Long,
    val note: String? = null,
    val attachmentUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
