package dev.egamberganov.finflow.ui.components

import androidx.annotation.DrawableRes
import dev.egamberganov.finflow.domain.model.AccountType
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.ui.theme.appColors

object CategoryVisuals {
    /**
     * Map category / icon identifier to our custom 24dp outline vector drawable.
     * Returns a valid @DrawableRes ID.
     */
    @DrawableRes
    fun getDrawableForName(iconName: String): Int {
        return when (iconName.lowercase().trim()) {
            "food", "restaurant", "dining", "food.svg" -> R.drawable.ic_cat_food
            "transport", "car", "bus", "transport.svg" -> R.drawable.ic_cat_transport
            "shopping", "cart", "bag", "shopping.svg" -> R.drawable.ic_cat_shopping
            "bills", "receipt", "utility", "bills.svg" -> R.drawable.ic_cat_bills
            "health", "medical", "health.svg" -> R.drawable.ic_cat_health
            "fitness", "gym", "sport" -> R.drawable.ic_cat_health
            "entertainment", "fun", "game", "entertainment.svg" -> R.drawable.ic_cat_entertainment
            "education", "school", "education.svg" -> R.drawable.ic_cat_education
            "travel", "trip", "map", "travel.svg" -> R.drawable.ic_cat_travel
            "home", "house", "rent", "home.svg" -> R.drawable.ic_cat_home
            "work", "job", "office", "freelance", "work.svg" -> R.drawable.ic_cat_work
            "salary", "payment", "income", "income.svg" -> R.drawable.ic_tx_income
            "expense", "expense.svg" -> R.drawable.ic_tx_expense
            "transfer", "transfer.svg" -> R.drawable.ic_tx_transfer
            "wallet", "cash", "wallet.svg" -> R.drawable.ic_fin_wallet
            "bank", "bank.svg" -> R.drawable.ic_fin_bank
            "savings", "piggy", "savings.svg" -> R.drawable.ic_fin_savings
            "investment", "investment.svg" -> R.drawable.ic_fin_investment
            "loan", "debt", "loan.svg" -> R.drawable.ic_fin_loan
            "money", "money.svg" -> R.drawable.ic_fin_money
            "coins", "coins.svg" -> R.drawable.ic_fin_coins
            "banknote", "banknote.svg" -> R.drawable.ic_fin_banknote
            "balance", "balance.svg" -> R.drawable.ic_fin_balance
            "financial-growth", "growth", "financial_growth" -> R.drawable.ic_fin_financial_growth
            "e-wallet", "ewallet", "mobile" -> R.drawable.ic_fin_ewallet
            "history", "history.svg" -> R.drawable.ic_tx_history
            "recurring", "recurring.svg" -> R.drawable.ic_tx_recurring
            "paid", "paid.svg" -> R.drawable.ic_tx_paid
            "pending", "pending.svg" -> R.drawable.ic_tx_pending
            "upcoming", "upcoming.svg" -> R.drawable.ic_tx_upcoming
            "attach-receipt", "attach_receipt" -> R.drawable.ic_act_attach_receipt
            "camera", "camera.svg" -> R.drawable.ic_act_camera
            "gallery", "gallery.svg" -> R.drawable.ic_act_gallery
            "download", "download.svg" -> R.drawable.ic_act_download
            "upload", "upload.svg" -> R.drawable.ic_act_upload
            "export", "export.svg" -> R.drawable.ic_act_export
            "import", "import.svg" -> R.drawable.ic_act_import
            "backup", "backup.svg" -> R.drawable.ic_act_backup
            "restore", "restore.svg" -> R.drawable.ic_act_restore
            "chart", "statistics", "chart.svg" -> R.drawable.ic_stat_chart
            "analytics", "analytics.svg" -> R.drawable.ic_stat_analytics
            "percentage", "percentage.svg" -> R.drawable.ic_stat_percentage
            "category-analysis", "category_analysis" -> R.drawable.ic_stat_category_analysis
            "income-trend", "income_trend" -> R.drawable.ic_stat_income_trend
            "expense-trend", "expense_trend" -> R.drawable.ic_stat_expense_trend
            "comparison", "comparison.svg" -> R.drawable.ic_stat_comparison
            "settings", "settings.svg" -> R.drawable.ic_nav_settings
            "notification", "notification.svg" -> R.drawable.ic_sys_notification
            "language", "language.svg" -> R.drawable.ic_sys_language
            "dark-theme", "dark_theme" -> R.drawable.ic_sys_dark_theme
            "light-theme", "light_theme" -> R.drawable.ic_sys_light_theme
            "info", "info.svg" -> R.drawable.ic_sys_info
            "help", "help.svg" -> R.drawable.ic_sys_help
            "contact", "contact.svg" -> R.drawable.ic_sys_contact
            "warning", "warning.svg" -> R.drawable.ic_sys_warning
            "error", "error.svg" -> R.drawable.ic_sys_error
            "success", "success.svg" -> R.drawable.ic_sys_success
            else -> R.drawable.ic_cat_other
        }
    }

    /**
     * Map account type to outline vector drawable.
     */
    @DrawableRes
    fun getAccountDrawable(accountType: String): Int {
        return AccountType.fromString(accountType).iconResId
    }

    // Standard fallback ImageVectors if needed
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
        return when (AccountType.fromString(accountType)) {
            AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
            AccountType.BANK -> Icons.Outlined.AccountBalance
            AccountType.SAVINGS -> Icons.Outlined.Savings
            AccountType.INVESTMENT -> Icons.Outlined.TrendingUp
            AccountType.LOAN_DEBT -> Icons.Outlined.CreditCard
            AccountType.EWALLET -> Icons.Outlined.Smartphone
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
        "entertainment" to "Entertainment",
        "education" to "Education",
        "travel" to "Travel",
        "home" to "Home & Housing",
        "work" to "Work & Career",
        "salary" to "Salary / Income",
        "investment" to "Investment",
        "savings" to "Savings",
        "wallet" to "Cash & Wallet",
        "bank" to "Bank Account",
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

    val accountTypes = AccountType.entries.map { it.dbKey to it.name }
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
        val drawableRes = CategoryVisuals.getDrawableForName(if (categoryName.isNotEmpty()) categoryName else iconName)
        Icon(
            painter = painterResource(id = drawableRes),
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
        val drawableRes = CategoryVisuals.getAccountDrawable(accountType)
        Icon(
            painter = painterResource(id = drawableRes),
            contentDescription = accountType,
            tint = baseColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
