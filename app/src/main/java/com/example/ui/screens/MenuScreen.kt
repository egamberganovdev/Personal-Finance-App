package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.AppSettingsEntity
import com.example.domain.model.AccountWithBalance
import com.example.domain.model.CurrencyFormatter
import com.example.ui.theme.appColors

@Composable
fun MenuScreen(
    accounts: List<AccountWithBalance>,
    categoriesCount: Int,
    scheduledPaymentsCount: Int,
    appSettings: AppSettingsEntity?,
    onOpenManageAccounts: () -> Unit,
    onOpenManageCategories: () -> Unit,
    onOpenManageScheduledPayments: () -> Unit,
    onOpenAppearanceDialog: () -> Unit,
    onOpenLanguageDialog: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenContactSupport: () -> Unit,
    onOpenTermsOfUse: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBalance = accounts.sumOf { it.currentBalance }
    val primaryCurrency = accounts.firstOrNull()?.account?.currency ?: "UZS"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 32.dp)
    ) {
        // Top Title
        Text(
            text = stringResource(R.string.nav_menu),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Profile / Storage Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.appColors.brand.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.brand,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = stringResource(R.string.my_finances),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.my_finances_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 1: FINANCIAL MANAGEMENT
        MenuSectionHeader(title = "FINANCIAL MANAGEMENT")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column {
                MenuItemRow(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = stringResource(R.string.accounts),
                    subtitle = stringResource(
                        R.string.cash_accounts_subtitle,
                        accounts.size,
                        CurrencyFormatter.formatAmount(totalBalance, primaryCurrency)
                    ),
                    onClick = onOpenManageAccounts,
                    testTag = "menu_accounts_item"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                MenuItemRow(
                    icon = Icons.Outlined.Category,
                    title = stringResource(R.string.categories),
                    subtitle = stringResource(R.string.default_categories_subtitle, categoriesCount),
                    onClick = onOpenManageCategories,
                    testTag = "menu_categories_item"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                MenuItemRow(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.scheduled_payments),
                    subtitle = if (scheduledPaymentsCount > 0) "$scheduledPaymentsCount active" else stringResource(R.string.no_scheduled_subtitle),
                    onClick = onOpenManageScheduledPayments,
                    testTag = "menu_scheduled_payments_item"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: APP SETTINGS
        MenuSectionHeader(title = "APP SETTINGS")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column {
                MenuItemRow(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.appearance),
                    subtitle = "${stringResource(R.string.theme_mode)}: ${appSettings?.themeMode ?: "System"}",
                    onClick = onOpenAppearanceDialog,
                    testTag = "menu_appearance_item"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                MenuItemRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.language),
                    subtitle = when (appSettings?.languageCode) {
                        "uz" -> "O'zbekcha"
                        "ru" -> "Русский"
                        else -> "English"
                    },
                    onClick = onOpenLanguageDialog,
                    testTag = "menu_language_item"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                // Notifications Switch Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.appColors.brand.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.appColors.brand,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notifications),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.payment_reminders),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = appSettings?.notificationsEnabled ?: true,
                        onCheckedChange = onToggleNotifications,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.appColors.brand
                        ),
                        modifier = Modifier.testTag("notifications_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: ABOUT & SUPPORT
        MenuSectionHeader(title = "ABOUT & SUPPORT")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column {
                MenuItemRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = stringResource(R.string.contact_support),
                    subtitle = "support@financeapp.local",
                    onClick = onOpenContactSupport,
                    testTag = "menu_support_item"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                MenuItemRow(
                    icon = Icons.Outlined.Policy,
                    title = stringResource(R.string.terms_of_use),
                    subtitle = "Terms & Privacy conditions",
                    onClick = onOpenTermsOfUse,
                    testTag = "menu_terms_item"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                MenuItemRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_app),
                    subtitle = "Version 1.0.0 · Local-first",
                    onClick = onOpenAbout,
                    testTag = "menu_about_item"
                )
            }
        }
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.appColors.brand.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.appColors.brand,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
