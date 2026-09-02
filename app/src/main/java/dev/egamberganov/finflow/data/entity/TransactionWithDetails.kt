package dev.egamberganov.finflow.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity?
)

data class ScheduledPaymentWithDetails(
    @Embedded val scheduledPayment: ScheduledPaymentEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity?
)
