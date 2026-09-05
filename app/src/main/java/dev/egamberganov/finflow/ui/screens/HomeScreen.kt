package dev.egamberganov.finflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.ScheduledPaymentWithDetails
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.domain.GreetingProvider
import dev.egamberganov.finflow.domain.model.AccountWithBalance
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.domain.model.FinancialSummary
import dev.egamberganov.finflow.ui.components.CategoryIconBadge
import dev.egamberganov.finflow.ui.components.CategoryVisuals
import dev.egamberganov.finflow.ui.theme.BrandPrimary
import dev.egamberganov.finflow.ui.theme.BrandPrimary700
import dev.egamberganov.finflow.ui.theme.BrandPrimaryVariant
import dev.egamberganov.finflow.ui.theme.appColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    summary: FinancialSummary,
    selectedAccount: AccountWithBalance?,
    recentTransactions: List<TransactionWithDetails>,
    upcomingPayments: List<ScheduledPaymentWithDetails>,
    onOpenAccountSelector: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onScheduledPaymentClick: (ScheduledPaymentWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Dynamic Time-Based Greeting Calculation (05:00-11:59 Morning, 12:00-17:59 Afternoon, 18:00-04:59 Night)
    val greeting = stringResource(GreetingProvider.getGreetingStringRes())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // Top Bar: Header with Prominent Greeting & Account Selector Chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.finance_overview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Selected Account Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenAccountSelector)
                    .testTag("home_account_selector_chip"),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedAccount != null) {
                                    CategoryVisuals.parseColor(selectedAccount.account.colorHex)
                                } else {
                                    MaterialTheme.appColors.brand
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedAccount?.account?.name ?: stringResource(R.string.all_accounts),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Balance Overview Card (Indigo–Violet Gradient)
        BalanceOverviewCard(
            summary = summary,
            selectedAccount = selectedAccount
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Upcoming Scheduled Payments Section
        if (upcomingPayments.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.upcoming_payments).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                upcomingPayments.take(4).forEach { payment ->
                    UpcomingPaymentCard(
                        payment = payment,
                        onClick = { onScheduledPaymentClick(payment) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Transactions Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_transactions).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onViewAllTransactions,
                modifier = Modifier.testTag("home_view_all_transactions_button")
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.appColors.brand
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.brand,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Recent Transactions List
        if (recentTransactions.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.no_recent_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentTransactions.take(5).forEach { tx ->
                    TransactionListItem(
                        item = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceOverviewCard(
    summary: FinancialSummary,
    selectedAccount: AccountWithBalance?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BrandPrimary,
                            BrandPrimaryVariant,
                            BrandPrimary700
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.available_balance),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEEECFB),
                        letterSpacing = 0.5.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (selectedAccount != null) {
                                Icon(
                                    painter = painterResource(id = selectedAccount.accountType.iconResId),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = selectedAccount?.account?.name ?: stringResource(R.string.all_accounts),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Large Balance Text with Currency
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = CurrencyFormatter.formatAmountWithoutSymbol(summary.totalBalance),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedAccount?.account?.currency ?: summary.primaryCurrency,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEEECFB),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Income & Expense Overview Row with Directional Icons & Signs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Income Column (with Upward Arrow ↗ and +)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stat_income_trend),
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.income).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD1FAE5),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "+${CurrencyFormatter.formatCompactAmount(summary.totalIncome, summary.primaryCurrency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }

                    // Expense Column (with Downward Arrow ↘ and -)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stat_expense_trend),
                                contentDescription = null,
                                tint = Color(0xFFFF8470),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.expense).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFEE2E2),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "-${CurrencyFormatter.formatCompactAmount(summary.totalExpense, summary.primaryCurrency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8470)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingPaymentCard(
    payment: ScheduledPaymentWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diffMillis = payment.scheduledPayment.nextPaymentDateMillis - System.currentTimeMillis()
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

    val daysRemainingText = when {
        diffDays <= 0 -> stringResource(R.string.due_today)
        else -> stringResource(R.string.in_days, diffDays)
    }

    Surface(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("upcoming_payment_${payment.scheduledPayment.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Bar (Upcoming Amber Indicator)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.appColors.upcoming)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.scheduled_payment).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.appColors.upcoming,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = payment.scheduledPayment.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = daysRemainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = CurrencyFormatter.formatCompactAmount(payment.scheduledPayment.amount, payment.scheduledPayment.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TransactionListItem(
    item: TransactionWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTransfer = item.transaction.type == "TRANSFER"
    val isExpense = item.transaction.type == "EXPENSE"
    val timeFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val categoryName = item.category?.name ?: ""
    val amountColor = when {
        isTransfer -> MaterialTheme.appColors.brand
        isExpense -> MaterialTheme.appColors.expense
        else -> MaterialTheme.appColors.income
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("transaction_item_${item.transaction.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 44px Icon Tile with 15% opacity background
            if (isTransfer) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.appColors.brand.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tx_transfer),
                            contentDescription = "Transfer",
                            tint = MaterialTheme.appColors.brand,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                CategoryIconBadge(
                    iconName = item.category?.iconName ?: "other",
                    colorHex = item.category?.colorHex ?: if (isExpense) "#F4543D" else "#22C55E",
                    categoryName = categoryName,
                    size = 44.dp,
                    iconSize = 22.dp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTransfer) {
                        "${item.account?.name ?: "Account"} → ${item.toAccount?.name ?: "Account"}"
                    } else {
                        item.category?.name ?: "Transaction"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = item.transaction.note?.ifBlank { null }
                        ?: "${item.account?.name ?: "Wallet"} · ${timeFormatter.format(Date(item.transaction.dateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = if (item.transaction.note != null) FontStyle.Italic else FontStyle.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Amount with explicit sign or Transfer arrow
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        id = when {
                            isTransfer -> R.drawable.ic_tx_transfer
                            isExpense -> R.drawable.ic_tx_expense
                            else -> R.drawable.ic_tx_income
                        }
                    ),
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val amountText = if (isTransfer) {
                    val destCurrency = item.toAccount?.currency ?: item.transaction.currency
                    if (item.transaction.convertedAmount != null && !item.transaction.currency.equals(destCurrency, ignoreCase = true)) {
                        "${CurrencyFormatter.formatAmount(item.transaction.amount, item.transaction.currency)} → ${CurrencyFormatter.formatAmount(item.transaction.convertedAmount, destCurrency)}"
                    } else {
                        CurrencyFormatter.formatAmount(item.transaction.amount, item.transaction.currency)
                    }
                } else {
                    CurrencyFormatter.formatAmount(
                        amount = item.transaction.amount,
                        currency = item.transaction.currency,
                        showSign = true,
                        isExpense = isExpense
                    )
                }
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
        }
    }
}
