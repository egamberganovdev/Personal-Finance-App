package dev.egamberganov.finflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "EXPENSE" or "INCOME"
    val iconName: String, // "food", "transport", "shopping", "bills", "health", "entertainment", "salary", "freelance", "business", "gift", "other"
    val colorHex: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
