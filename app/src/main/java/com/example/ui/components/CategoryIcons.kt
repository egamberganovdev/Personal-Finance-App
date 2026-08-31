package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.appColors

object CategoryVisuals {
    // Standard Lucide-style rounded outlined icons
    fun getIconForName(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "food", "restaurant", "dining" -> Icons.Outlined.Restaurant
            "transport", "car", "bus" -> Icons.Outlined.DirectionsCar
            "shopping", "cart", "bag" -> Icons.Outlined.ShoppingCart
            "bills", "receipt", "utility" -> Icons.Outlined.Receipt
            "health", "medical" -> Icons.Outlined.MedicalServices
            "fitness", "gym", "sport" -> Icons.Outlined.FitnessCenter
            "entertainment", "fun", "game" -> Icons.Outlined.SportsEsports
            "salary", "payment", "income" -> Icons.Outlined.Payments
            "freelance", "work", "laptop", "tech" -> Icons.Outlined.Laptop
            "business", "store" -> Icons.Outlined.Store
            "gift", "donation" -> Icons.Outlined.CardGiftcard
            "savings", "piggy" -> Icons.Outlined.Savings
            "investment" -> Icons.Outlined.TrendingUp
            "wallet", "cash" -> Icons.Outlined.AccountBalanceWallet
            "bank" -> Icons.Outlined.AccountBalance
            else -> Icons.Outlined.Category
        }
    }

    // Account Type Icons
    fun getAccountIcon(accountType: String): ImageVector {
        return when (accountType.lowercase()) {
            "cash", "wallet" -> Icons.Outlined.AccountBalanceWallet
            "bank" -> Icons.Outlined.AccountBalance
            "savings" -> Icons.Outlined.Savings
            "investment" -> Icons.Outlined.TrendingUp
            "loan", "debt" -> Icons.Outlined.CreditCard
            "e-wallet", "ewallet", "mobile" -> Icons.Outlined.Smartphone
            else -> Icons.Outlined.AccountBalanceWallet
        }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF6C5CE7)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            when (cleanHex.length) {
                6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                else -> fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }

    // Dynamic light/dark color mapping based on design system
    fun getCategoryDynamicColor(categoryName: String, iconName: String, isDark: Boolean): Color {
        val key = (categoryName.ifEmpty { iconName }).lowercase()
        return when {
            key.contains("food") || key.contains("restaurant") -> if (isDark) Color(0xFFFFA466) else Color(0xFFFF8A3D)
            key.contains("transport") || key.contains("car") || key.contains("bus") -> if (isDark) Color(0xFF8B7CF6) else Color(0xFF6C5CE7)
            key.contains("shopping") || key.contains("cart") || key.contains("bag") -> if (isDark) Color(0xFFFF7FA6) else Color(0xFFFF5C8A)
            key.contains("bill") || key.contains("receipt") -> if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
            key.contains("entertain") || key.contains("game") || key.contains("fun") -> if (isDark) Color(0xFFC084FC) else Color(0xFFA855F7)
            key.contains("health") || key.contains("medical") || key.contains("fitness") -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF14B8A6)
            key.contains("salary") -> if (isDark) Color(0xFF4ADE80) else Color(0xFF22C55E)
            key.contains("freelance") || key.contains("laptop") || key.contains("work") -> if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
            key.contains("gift") -> if (isDark) Color(0xFFF472B6) else Color(0xFFEC4899)
            key.contains("other") -> if (isDark) Color(0xFFB0B8C8) else Color(0xFF94A3B8)
            else -> if (isDark) Color(0xFF8B7CF6) else Color(0xFF6C5CE7)
        }
    }

    val availableIcons = listOf(
        "food" to "Food & Dining",
        "transport" to "Transport",
        "shopping" to "Shopping",
        "bills" to "Bills & Utilities",
        "health" to "Health & Medical",
        "fitness" to "Fitness & Sports",
        "entertainment" to "Entertainment",
        "salary" to "Salary",
        "freelance" to "Freelance & Tech",
        "business" to "Business",
        "gift" to "Gifts & Donations",
        "other" to "General / Other"
    )

    // Palette hex values for pickers (aligned with Design System)
    val availableColors = listOf(
        "#FF8A3D", // Food Orange
        "#6C5CE7", // Indigo-Violet (Brand)
        "#FF5C8A", // Shopping Pink
        "#EF4444", // Bills Red
        "#A855F7", // Entertainment Purple
        "#14B8A6", // Health Teal
        "#22C55E", // Salary Green
        "#3B82F6", // Freelance Blue
        "#EC4899", // Gift Deep Pink
        "#94A3B8"  // Other Slate
    )

    val accountTypes = listOf(
        "Cash" to "Cash / Wallet",
        "Bank" to "Bank Account",
        "Savings" to "Savings",
        "Investment" to "Investment",
        "Loan" to "Loan / Debt",
        "E-Wallet" to "E-Wallet"
    )
}

/**
 * 44px Icon Tile with ~15% alpha background and crisp Lucide-styled icon
 * Adheres strictly to Design System specs for both Light and Dark modes.
 */
@Composable
fun CategoryIconBadge(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    categoryName: String = "",
    useDynamicDarkVariant: Boolean = true
) {
    val isDark = MaterialTheme.appColors.isDark
    val baseColor = if (useDynamicDarkVariant && categoryName.isNotEmpty()) {
        CategoryVisuals.getCategoryDynamicColor(categoryName, iconName, isDark)
    } else {
        val parsed = CategoryVisuals.parseColor(colorHex)
        if (isDark) {
            // Provide lighter variant for dark mode if raw color
            parsed
        } else {
            parsed
        }
    }

    val tileBgColor = baseColor.copy(alpha = if (isDark) 0.18f else 0.12f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(tileBgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryVisuals.getIconForName(iconName),
            contentDescription = categoryName.ifEmpty { iconName },
            tint = baseColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Account Badge with Lucide-styled icon and 15% opacity background
 */
@Composable
fun AccountTypeBadge(
    accountType: String,
    colorHex: String = "#6C5CE7",
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    val isDark = MaterialTheme.appColors.isDark
    val baseColor = CategoryVisuals.parseColor(colorHex, fallback = MaterialTheme.appColors.brand)
    val tileBgColor = baseColor.copy(alpha = if (isDark) 0.18f else 0.12f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(tileBgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryVisuals.getAccountIcon(accountType),
            contentDescription = accountType,
            tint = baseColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
