package dev.egamberganov.finflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.domain.model.DateFilterRange
import dev.egamberganov.finflow.domain.model.TransactionFilterType
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
    typeFilter: TransactionFilterType,
    dateRange: DateFilterRange,
    selectedCategoryId: Long?,
    searchQuery: String,
    onTypeFilterChange: (TransactionFilterType) -> Unit,
    onDateRangeChange: (DateFilterRange) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var isDateDropdownExpanded by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

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
        // Top Bar
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) onSearchQueryChange("")
                    },
                    modifier = Modifier.testTag("transactions_search_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Outlined.Clear else Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(visible = isSearchExpanded) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactions_search_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_transactions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Type Filter Chips: All | Income | Expense
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTypeFilterChange(filter) }
                        .testTag("filter_chip_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) activeBg else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
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

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Filters: Date Range & Category Dropdowns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date Range Dropdown Anchor
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isDateDropdownExpanded = true }
                        .testTag("date_range_filter_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (dateRange) {
                                DateFilterRange.THIS_MONTH -> stringResource(R.string.this_month)
                                DateFilterRange.THIS_WEEK -> stringResource(R.string.this_week)
                                DateFilterRange.THIS_YEAR -> stringResource(R.string.this_year)
                                DateFilterRange.ALL_TIME -> stringResource(R.string.all_time)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "▾",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = isDateDropdownExpanded,
                    onDismissRequest = { isDateDropdownExpanded = false }
                ) {
                    DateFilterRange.values().forEach { range ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (range) {
                                        DateFilterRange.THIS_MONTH -> stringResource(R.string.this_month)
                                        DateFilterRange.THIS_WEEK -> stringResource(R.string.this_week)
                                        DateFilterRange.THIS_YEAR -> stringResource(R.string.this_year)
                                        DateFilterRange.ALL_TIME -> stringResource(R.string.all_time)
                                    }
                                )
                            },
                            onClick = {
                                onDateRangeChange(range)
                                isDateDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Category Filter Dropdown Anchor
            Box(modifier = Modifier.weight(1f)) {
                val selectedCatName = categories.find { it.id == selectedCategoryId }?.name
                    ?: stringResource(R.string.categories)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isCategoryDropdownExpanded = true }
                        .testTag("category_filter_dropdown_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCategoryId != null) MaterialTheme.appColors.brand.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (selectedCategoryId != null) MaterialTheme.appColors.brand.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedCatName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCategoryId != null) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "▾",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_all)) },
                        onClick = {
                            onCategoryFilterChange(null)
                            isCategoryDropdownExpanded = false
                        }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                onCategoryFilterChange(cat.id)
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transaction Groups
        if (transactions.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
}
