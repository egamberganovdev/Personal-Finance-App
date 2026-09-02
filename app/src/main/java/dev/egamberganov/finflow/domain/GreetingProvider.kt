package dev.egamberganov.finflow.domain

import androidx.annotation.StringRes
import dev.egamberganov.finflow.R
import java.util.Calendar

/**
 * Reusable domain helper for determining dynamic time-based greetings
 * using the user's current local device time.
 *
 * Rules:
 * - 05:00–11:59 → "Good morning"
 * - 12:00–17:59 → "Good afternoon"
 * - 18:00–04:59 → "Good night"
 */
object GreetingProvider {

    enum class TimeOfDay(@param:StringRes val stringRes: Int) {
        MORNING(R.string.greeting_morning),
        AFTERNOON(R.string.greeting_afternoon),
        NIGHT(R.string.greeting_night)
    }

    /**
     * Determines the greeting enum based on the current local device time or specified hour of day (0-23).
     */
    fun getTimeOfDay(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): TimeOfDay {
        return when (hourOfDay) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            else -> TimeOfDay.NIGHT
        }
    }

    @StringRes
    fun getGreetingStringRes(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Int {
        return getTimeOfDay(hourOfDay).stringRes
    }
}
