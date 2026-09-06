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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.domain.model.AccountWithBalance
import dev.egamberganov.finflow.domain.model.DateFilterRange
import dev.egamberganov.finflow.domain.model.TransactionFilterType
import dev.egamberganov.finflow.ui.components.FilterTransactionsSheet
import dev.egamberganov.finflow.ui.theme.appColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionWithDetails>,
    categories: List<CategoryEntity>,
    accounts: List<AccountWithBalance>,
    typeFilter: TransactionFilterType,
    filterAccountId: Long?,
    selectedCategoryId: Long?,
    dateRange: DateFilterRange,
    customStartDateMillis: Long?,
    customEndDateMillis: Long?,
    minAmount: Long?,
    maxAmount: Long?,
    activeFiltersCount: Int,
    searchQuery: String,
    onTypeFilterChange: (TransactionFilterType) -> Unit,
    onAccountFilterChange: (Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onDateRangeChange: (DateFilterRange) -> Unit,
    onCustomDateRangeChange: (Long?, Long?) -> Unit,
    onAmountRangeChange: (Long?, Long?) -> Unit,
    onApplyAdvancedFilters: (
        type: TransactionFilterType,
        accountId: Long?,
        categoryId: Long?,
        dateRange: DateFilterRange,
        customStartMillis: Long?,
        customEndMillis: Long?,
        minAmount: Long?,
        maxAmount: Long?
    ) -> Unit,
    onClearAllFilters: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    // Date grouping helper
    val dayKeyFormatter = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val dayDisplayFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val todayKey = remember { dayKeyFormatter.format(Date()) }
    val yesterdayKey = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        dayKeyFormatter.format(cal.time)
    }

    val groupedTransactions = remember(transactions) {
        transactions.groupBy { tx ->
            dayKeyFormatter.format(Date(tx.transaction.dateMillis))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        // Top Bar: Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_transactions),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (activeFiltersCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.appColors.brand.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.appColors.brand.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = stringResource(R.string.active_filters_count, activeFiltersCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.appColors.brand,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar & Filter Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-visibility Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("transactions_search_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_transactions),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.appColors.brand
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_search),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.testTag("btn_clear_search")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            // Filter Button with Badge
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showFilterSheet = true }
                    .testTag("transactions_filter_button"),
                shape = RoundedCornerShape(16.dp),
                color = if (activeFiltersCount > 0) MaterialTheme.appColors.brand.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (activeFiltersCount > 0) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_filter),
                        contentDescription = "Filter",
                        tint = if (activeFiltersCount > 0) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )

                    // Active filters indicator dot/pill
                    if (activeFiltersCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.appColors.brand)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Type Filter Chips: All | Expense | Income | Transfer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionFilterType.values().forEach { filter ->
                val isSelected = typeFilter == filter
                val activeBg = when (filter) {
                    TransactionFilterType.ALL -> MaterialTheme.appColors.brand
                    TransactionFilterType.INCOME -> MaterialTheme.appColors.income
                    TransactionFilterType.EXPENSE -> MaterialTheme.appColors.expense
                    TransactionFilterType.TRANSFER -> MaterialTheme.appColors.brand
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTypeFilterChange(filter) }
                        .testTag("filter_chip_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) activeBg else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (filter) {
                                TransactionFilterType.ALL -> stringResource(R.string.filter_all)
                                TransactionFilterType.INCOME -> stringResource(R.string.filter_income)
                                TransactionFilterType.EXPENSE -> stringResource(R.string.filter_expense)
                                TransactionFilterType.TRANSFER -> stringResource(R.string.filter_transfer)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Filters Summary Bar
        if (activeFiltersCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.appColors.brand.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_filter),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.brand,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.filtered_results_summary, activeFiltersCount, transactions.size),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.appColors.brand
                    )
                }

                TextButton(
                    onClick = onClearAllFilters,
                    modifier = Modifier.testTag("btn_clear_active_filters")
                ) {
                    Text(
                        text = stringResource(R.string.clear_filters),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.appColors.brand
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction List / Empty States
        if (transactions.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        // Empty State 1: Search returned no results
                        searchQuery.isNotBlank() -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_search_results_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.no_search_results_desc, searchQuery),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onClearSearch,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_empty_clear_search")
                            ) {
                                Text(text = stringResource(R.string.clear_search))
                            }
                        }

                        // Empty State 2: Filters returned no results
                        activeFiltersCount > 0 -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_filter),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_filter_results_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.no_filter_results_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onClearAllFilters,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_empty_clear_filters")
                            ) {
                                Text(text = stringResource(R.string.clear_filters))
                            }
                        }

                        // Empty State 3: No transactions at all
                        else -> {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_transactions_found),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.add_first_transaction),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedTransactions.forEach { (dateKey, itemsInDate) ->
                    item(key = "header_$dateKey") {
                        val headerText = when (dateKey) {
                            todayKey -> stringResource(R.string.today)
                            yesterdayKey -> stringResource(R.string.yesterday)
                            else -> {
                                val firstItem = itemsInDate.firstOrNull()
                                if (firstItem != null) {
                                    dayDisplayFormatter.format(Date(firstItem.transaction.dateMillis))
                                } else dateKey
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.entries_count, itemsInDate.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    items(itemsInDate, key = { it.transaction.id }) { item ->
                        TransactionListItem(
                            item = item,
                            onClick = { onTransactionClick(item) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Advanced Filters BottomSheet
    if (showFilterSheet) {
        FilterTransactionsSheet(
            initialType = typeFilter,
            initialAccountId = filterAccountId,
            initialCategoryId = selectedCategoryId,
            initialDateRange = dateRange,
            initialCustomStartMillis = customStartDateMillis,
            initialCustomEndMillis = customEndDateMillis,
            initialMinAmount = minAmount,
            initialMaxAmount = maxAmount,
            accounts = accounts,
            categories = categories,
            onApply = { type, accountId, categoryId, dRange, customStart, customEnd, min, max ->
                onApplyAdvancedFilters(type, accountId, categoryId, dRange, customStart, customEnd, min, max)
            },
            onReset = {
                onClearAllFilters()
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}
