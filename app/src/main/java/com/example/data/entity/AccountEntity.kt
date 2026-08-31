package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = "Cash", // Cash in V1
    val currency: String = "UZS",
    val initialBalance: Long = 0L, // In standard monetary units
    val colorHex: String = "#5E5CE6",
    val createdAt: Long = System.currentTimeMillis()
)
