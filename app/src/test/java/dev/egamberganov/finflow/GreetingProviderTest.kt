package dev.egamberganov.finflow

import dev.egamberganov.finflow.domain.GreetingProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingProviderTest {

    @Test
    fun testMorningHours() {
        for (hour in 5..11) {
            assertEquals(
                "Hour $hour should be MORNING",
                GreetingProvider.TimeOfDay.MORNING,
                GreetingProvider.getTimeOfDay(hour)
            )
            assertEquals(
                R.string.greeting_morning,
                GreetingProvider.getGreetingStringRes(hour)
            )
        }
    }

    @Test
    fun testAfternoonHours() {
        for (hour in 12..17) {
            assertEquals(
                "Hour $hour should be AFTERNOON",
                GreetingProvider.TimeOfDay.AFTERNOON,
                GreetingProvider.getTimeOfDay(hour)
            )
            assertEquals(
                R.string.greeting_afternoon,
                GreetingProvider.getGreetingStringRes(hour)
            )
        }
    }

    @Test
    fun testNightHours() {
        // 18:00 - 23:59 and 00:00 - 04:59
        val nightHours = (18..23) + (0..4)
        for (hour in nightHours) {
            assertEquals(
                "Hour $hour should be NIGHT",
                GreetingProvider.TimeOfDay.NIGHT,
                GreetingProvider.getTimeOfDay(hour)
            )
            assertEquals(
                R.string.greeting_night,
                GreetingProvider.getGreetingStringRes(hour)
            )
        }
    }
}
