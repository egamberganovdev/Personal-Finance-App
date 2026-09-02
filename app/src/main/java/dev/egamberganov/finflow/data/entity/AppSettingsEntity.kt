package dev.egamberganov.finflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isOnboardingCompleted: Boolean = false,
    val selectedAccountId: Long? = null, // null means "All Accounts"
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val languageCode: String = "en", // "en", "uz", "ru"
    val notificationsEnabled: Boolean = true
)
