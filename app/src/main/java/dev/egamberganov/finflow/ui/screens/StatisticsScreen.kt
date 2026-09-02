package dev.egamberganov.finflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.domain.model.CategorySpend
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.domain.model.PeriodActivity
import dev.egamberganov.finflow.domain.model.TimePeriod
import dev.egamberganov.finflow.ui.components.CategoryIconBadge
import dev.egamberganov.finflow.ui.components.CategoryVisuals
import dev.egamberganov.finflow.ui.theme.appColors
import java.util.Locale

@Composable
fun StatisticsScreen(
    selectedPeriod: TimePeriod,
    periodActivities: List<PeriodActivity>,
    expenseCategorySpends: List<CategorySpend>,
    incomeCategorySpends: List<CategorySpend>,
    primaryCurrency: String,
    onPeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = remember(periodActivities) { periodActivities.sumOf { it.income } }
    val totalExpense = remember(periodActivities) { periodActivities.sumOf { it.expense } }
    val netChange = totalIncome - totalExpense

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // Top Title
        Text(
            text = stringResource(R.string.nav_statistics),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Time Period Filter Chips: [Week | Month | Year | Custom]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimePeriod.values().forEach { period ->
                val isSelected = selectedPeriod == period

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPeriodChange(period) }
                        .testTag("stat_period_${period.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (period) {
                                TimePeriod.WEEK -> stringResource(R.string.period_week)
                                TimePeriod.MONTH -> stringResource(R.string.period_month)
                                TimePeriod.YEAR -> stringResource(R.string.period_year)
                                TimePeriod.CUSTOM -> stringResource(R.string.period_custom)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3 Summary Metric Cards in a Row: Income | Expense | Net
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Income Metric Card
            StatMetricCard(
                label = stringResource(R.string.total_income),
                amount = totalIncome,
                currency = primaryCurrency,
                color = MaterialTheme.appColors.income,
                modifier = Modifier.weight(1f)
            )

            // Expense Metric Card
            StatMetricCard(
                label = stringResource(R.string.total_expense),
                amount = totalExpense,
                currency = primaryCurrency,
                color = MaterialTheme.appColors.expense,
                modifier = Modifier.weight(1f)
            )

            // Net Balance Metric Card
            StatMetricCard(
                label = stringResource(R.string.net_balance_change),
                amount = netChange,
                currency = primaryCurrency,
                color = if (netChange >= 0) MaterialTheme.appColors.income else MaterialTheme.appColors.expense,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Income vs Expense Bar Chart Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.income_vs_expense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Legend: Income / Expense
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.appColors.income)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.total_income),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.appColors.expense)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.total_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Custom Bar Chart Canvas
                IncomeExpenseBarChart(
                    activities = periodActivities,
                    incomeColor = MaterialTheme.appColors.income,
                    expenseColor = MaterialTheme.appColors.expense,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Expenses by Category Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.expenses_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (expenseCategorySpends.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_statistics_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Category Donut Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryDonutChart(
                            spends = expenseCategorySpends,
                            modifier = Modifier.size(160.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = CurrencyFormatter.formatCompactAmount(totalExpense, primaryCurrency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.total_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Breakdown List
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        expenseCategorySpends.forEach { spend ->
                            CategorySpendItem(
                                spend = spend,
                                currency = primaryCurrency
                            )
                        }
                    }
                }
            }
        }

        // Income by Category Section
        if (incomeCategorySpends.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.income_by_category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        incomeCategorySpends.forEach { spend ->
                            CategorySpendItem(
                                spend = spend,
                                currency = primaryCurrency
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    amount: Long,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = CurrencyFormatter.formatCompactAmount(amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun IncomeExpenseBarChart(
    activities: List<PeriodActivity>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(activities) {
        val highest = activities.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1L
        if (highest == 0L) 1L else highest
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        activities.forEach { act ->
            val incomeRatio = (act.income.toFloat() / maxVal).coerceIn(0.05f, 1f)
            val expenseRatio = (act.expense.toFloat() / maxVal).coerceIn(0.05f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Income Bar
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height((110 * incomeRatio).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(incomeColor)
                    )

                    // Expense Bar
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height((110 * expenseRatio).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(expenseColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = act.periodLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryDonutChart(
    spends: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.appColors.isDark
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = 28.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        spends.forEach { spend ->
            val sweepAngle = (spend.percentage / 100f) * 360f
            val color = CategoryVisuals.getCategoryDynamicColor(spend.category.name, spend.category.iconName, isDark)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = (sweepAngle - 2f).coerceAtLeast(0.1f),
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategorySpendItem(
    spend: CategorySpend,
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(
            iconName = spend.category.iconName,
            colorHex = spend.category.colorHex,
            categoryName = spend.category.name,
            size = 40.dp,
            iconSize = 20.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spend.category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", spend.percentage)}% · ${spend.transactionCount} transactions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = CurrencyFormatter.formatAmount(spend.totalAmount, currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
