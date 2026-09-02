package dev.egamberganov.finflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom Semantic Financial Colors Interface for Light & Dark
data class AppSemanticColors(
    val brand: Color,
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val upcoming: Color,
    val upcomingContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val isDark: Boolean
)

val LocalAppSemanticColors = staticCompositionLocalOf {
    AppSemanticColors(
        brand = BrandPrimary,
        income = IncomeGreenLight,
        incomeContainer = IncomeGreenLight.copy(alpha = 0.12f),
        expense = ExpenseRedLight,
        expenseContainer = ExpenseRedLight.copy(alpha = 0.12f),
        upcoming = UpcomingAmberLight,
        upcomingContainer = UpcomingAmberLight.copy(alpha = 0.12f),
        info = InfoBlueLight,
        infoContainer = InfoBlueLight.copy(alpha = 0.12f),
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textTertiary = LightTextTertiary,
        cardBackground = LightSurface,
        cardBorder = LightCardBorder,
        isDark = false
    )
}

val MaterialTheme.appColors: AppSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppSemanticColors.current

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color(0xFF161722),
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFEEECFB),
    secondary = IncomeGreenDark,
    onSecondary = Color(0xFF0E1016),
    secondaryContainer = IncomeGreenDark.copy(alpha = 0.15f),
    onSecondaryContainer = IncomeGreenDark,
    tertiary = BrandPrimaryLight,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = ExpenseRedDark,
    onError = Color(0xFF161722)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = IncomeGreenLight,
    onSecondary = Color.White,
    secondaryContainer = IncomeGreenLight.copy(alpha = 0.12f),
    onSecondaryContainer = Color(0xFF14532D),
    tertiary = BrandPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = ExpenseRedLight,
    onError = Color.White
)

@Composable
fun PersonalFinanceTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val semanticColors = if (darkTheme) {
        AppSemanticColors(
            brand = BrandPrimaryLight,
            income = IncomeGreenDark,
            incomeContainer = IncomeGreenDark.copy(alpha = 0.15f),
            expense = ExpenseRedDark,
            expenseContainer = ExpenseRedDark.copy(alpha = 0.15f),
            upcoming = UpcomingAmberDark,
            upcomingContainer = UpcomingAmberDark.copy(alpha = 0.15f),
            info = InfoBlueDark,
            infoContainer = InfoBlueDark.copy(alpha = 0.15f),
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textTertiary = DarkTextTertiary,
            cardBackground = DarkSurface,
            cardBorder = DarkCardBorder,
            isDark = true
        )
    } else {
        AppSemanticColors(
            brand = BrandPrimary,
            income = IncomeGreenLight,
            incomeContainer = IncomeGreenLight.copy(alpha = 0.12f),
            expense = ExpenseRedLight,
            expenseContainer = ExpenseRedLight.copy(alpha = 0.12f),
            upcoming = UpcomingAmberLight,
            upcomingContainer = UpcomingAmberLight.copy(alpha = 0.12f),
            info = InfoBlueLight,
            infoContainer = InfoBlueLight.copy(alpha = 0.12f),
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textTertiary = LightTextTertiary,
            cardBackground = LightSurface,
            cardBorder = LightCardBorder,
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalAppSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
