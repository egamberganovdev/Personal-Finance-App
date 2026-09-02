package dev.egamberganov.finflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.egamberganov.finflow.data.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettingsEntity)

    @Query("UPDATE app_settings SET selectedAccountId = :accountId WHERE id = 1")
    suspend fun setSelectedAccountId(accountId: Long?)

    @Query("UPDATE app_settings SET isOnboardingCompleted = :isCompleted WHERE id = 1")
    suspend fun setOnboardingCompleted(isCompleted: Boolean)

    @Query("UPDATE app_settings SET themeMode = :themeMode WHERE id = 1")
    suspend fun setThemeMode(themeMode: String)

    @Query("UPDATE app_settings SET languageCode = :languageCode WHERE id = 1")
    suspend fun setLanguageCode(languageCode: String)

    @Query("UPDATE app_settings SET notificationsEnabled = :enabled WHERE id = 1")
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
