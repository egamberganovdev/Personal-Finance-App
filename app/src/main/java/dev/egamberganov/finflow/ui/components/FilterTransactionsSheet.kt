package dev.egamberganov.finflow.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.domain.model.AccountWithBalance
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.domain.model.DateFilterRange
import dev.egamberganov.finflow.domain.model.TransactionFilterType
import dev.egamberganov.finflow.ui.theme.appColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTransactionsSheet(
    initialType: TransactionFilterType,
    initialAccountId: Long?,
    initialCategoryId: Long?,
    initialDateRange: DateFilterRange,
    initialCustomStartMillis: Long?,
    initialCustomEndMillis: Long?,
    initialMinAmount: Long?,
    initialMaxAmount: Long?,
    accounts: List<AccountWithBalance>,
    categories: List<CategoryEntity>,
    onApply: (
        type: TransactionFilterType,
        accountId: Long?,
        categoryId: Long?,
        dateRange: DateFilterRange,
        customStartMillis: Long?,
        customEndMillis: Long?,
        minAmount: Long?,
        maxAmount: Long?
    ) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    var tempType by remember { mutableStateOf(initialType) }
    var tempAccountId by remember { mutableStateOf(initialAccountId) }
    var tempCategoryId by remember { mutableStateOf(initialCategoryId) }
    var tempDateRange by remember { mutableStateOf(initialDateRange) }
    var tempStartMillis by remember { mutableStateOf(initialCustomStartMillis) }
    var tempEndMillis by remember { mutableStateOf(initialCustomEndMillis) }
    var tempMinText by remember { mutableStateOf(initialMinAmount?.toString() ?: "") }
    var tempMaxText by remember { mutableStateOf(initialMaxAmount?.toString() ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = Modifier.testTag("filter_transactions_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.appColors.brand.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_filter),
                            contentDescription = null,
                            tint = MaterialTheme.appColors.brand,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.filter_transactions),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_filter_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Transaction Type
            Text(
                text = stringResource(R.string.filter_by_type),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionFilterType.values().forEach { type ->
                    val selected = tempType == type
                    val label = when (type) {
                        TransactionFilterType.ALL -> stringResource(R.string.filter_all)
                        TransactionFilterType.INCOME -> stringResource(R.string.filter_income)
                        TransactionFilterType.EXPENSE -> stringResource(R.string.filter_expense)
                        TransactionFilterType.TRANSFER -> stringResource(R.string.filter_transfer)
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            tempType = type
                            // If user switched to TRANSFER, clear category since transfers don't use categories
                            if (type == TransactionFilterType.TRANSFER) {
                                tempCategoryId = null
                            }
                        },
                        label = { Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.appColors.brand,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("filter_chip_type_${type.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Account Filter
            Text(
                text = stringResource(R.string.filter_by_account),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Accounts" chip
                val isAllAccountsSelected = tempAccountId == null
                FilterChip(
                    selected = isAllAccountsSelected,
                    onClick = { tempAccountId = null },
                    label = { Text(text = stringResource(R.string.all_accounts), fontWeight = if (isAllAccountsSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.appColors.brand,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("filter_chip_account_all")
                )

                accounts.forEach { acc ->
                    val isSelected = tempAccountId == acc.id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { tempAccountId = acc.id }
                            .testTag("filter_chip_account_${acc.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.appColors.brand.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccountTypeBadge(
                                accountType = acc.type,
                                colorHex = acc.colorHex,
                                size = 26.dp,
                                iconSize = 14.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = acc.currency,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Category Filter (applicable when Type != TRANSFER)
            if (tempType != TransactionFilterType.TRANSFER) {
                Text(
                    text = stringResource(R.string.filter_by_category),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAllCategoriesSelected = tempCategoryId == null
                    FilterChip(
                        selected = isAllCategoriesSelected,
                        onClick = { tempCategoryId = null },
                        label = { Text(text = stringResource(R.string.all_categories), fontWeight = if (isAllCategoriesSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.appColors.brand,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("filter_chip_category_all")
                    )

                    // Filter categories to show relevant ones if Income or Expense is selected
                    val displayCategories = when (tempType) {
                        TransactionFilterType.INCOME -> categories.filter { it.type == "INCOME" }
                        TransactionFilterType.EXPENSE -> categories.filter { it.type == "EXPENSE" }
                        else -> categories
                    }

                    displayCategories.forEach { cat ->
                        val isSelected = tempCategoryId == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { tempCategoryId = cat.id },
                            leadingIcon = {
                                CategoryIconBadge(
                                    iconName = cat.iconName,
                                    colorHex = cat.colorHex,
                                    categoryName = cat.name,
                                    size = 20.dp,
                                    iconSize = 12.dp
                                )
                            },
                            label = { Text(text = cat.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.appColors.brand,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("filter_chip_category_${cat.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 4: Date Range
            Text(
                text = stringResource(R.string.filter_by_date),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilterRange.values().forEach { range ->
                    val selected = tempDateRange == range
                    val label = when (range) {
                        DateFilterRange.ALL_TIME -> stringResource(R.string.all_time)
                        DateFilterRange.TODAY -> stringResource(R.string.today)
                        DateFilterRange.THIS_WEEK -> stringResource(R.string.this_week)
                        DateFilterRange.THIS_MONTH -> stringResource(R.string.this_month)
                        DateFilterRange.THIS_YEAR -> stringResource(R.string.this_year)
                        DateFilterRange.CUSTOM -> stringResource(R.string.custom_range)
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            tempDateRange = range
                            if (range == DateFilterRange.CUSTOM) {
                                if (tempStartMillis == null) {
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.DAY_OF_MONTH, 1)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }
                                    tempStartMillis = cal.timeInMillis
                                }
                                if (tempEndMillis == null) {
                                    tempEndMillis = System.currentTimeMillis()
                                }
                            }
                        },
                        label = { Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.appColors.brand,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("filter_chip_date_${range.name.lowercase()}")
                    )
                }
            }

            // If Custom Date Range selected, show Start Date & End Date pickers
            if (tempDateRange == DateFilterRange.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Date Box
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply {
                                    tempStartMillis?.let { timeInMillis = it }
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val picked = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        tempStartMillis = picked
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .testTag("btn_custom_start_date"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.brand,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.start_date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = tempStartMillis?.let { dateFormatter.format(it) } ?: "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // End Date Box
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply {
                                    tempEndMillis?.let { timeInMillis = it }
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val picked = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }.timeInMillis
                                        tempEndMillis = picked
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .testTag("btn_custom_end_date"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.brand,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.end_date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = tempEndMillis?.let { dateFormatter.format(it) } ?: "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 5: Amount Range Filter
            Text(
                text = stringResource(R.string.filter_by_amount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tempMinText,
                    onValueChange = {
                        val cleaned = it.filter { char -> char.isDigit() }
                        tempMinText = cleaned
                        validationError = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_filter_min_amount"),
                    label = { Text(stringResource(R.string.min_amount)) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.appColors.brand,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = tempMaxText,
                    onValueChange = {
                        val cleaned = it.filter { char -> char.isDigit() }
                        tempMaxText = cleaned
                        validationError = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_filter_max_amount"),
                    label = { Text(stringResource(R.string.max_amount)) },
                    placeholder = { Text("∞") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.appColors.brand,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }

            // Validation Error Message
            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.appColors.expense
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons (Reset & Apply)
            val invalidAmountErrorMsg = stringResource(R.string.invalid_amount_range_error)
            val invalidDateErrorMsg = stringResource(R.string.invalid_date_range_error)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        tempType = TransactionFilterType.ALL
                        tempAccountId = null
                        tempCategoryId = null
                        tempDateRange = DateFilterRange.ALL_TIME
                        tempStartMillis = null
                        tempEndMillis = null
                        tempMinText = ""
                        tempMaxText = ""
                        validationError = null
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_reset_filters"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.reset_filters))
                }

                Button(
                    onClick = {
                        val parsedMin = tempMinText.toLongOrNull()
                        val parsedMax = tempMaxText.toLongOrNull()

                        if (parsedMin != null && parsedMax != null && parsedMin > parsedMax) {
                            validationError = invalidAmountErrorMsg
                            return@Button
                        }

                        if (tempDateRange == DateFilterRange.CUSTOM && tempStartMillis != null && tempEndMillis != null && tempStartMillis!! > tempEndMillis!!) {
                            validationError = invalidDateErrorMsg
                            return@Button
                        }

                        onApply(
                            tempType,
                            tempAccountId,
                            tempCategoryId,
                            tempDateRange,
                            tempStartMillis,
                            tempEndMillis,
                            parsedMin,
                            parsedMax
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.8f)
                        .height(50.dp)
                        .testTag("btn_apply_filters"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.brand)
                ) {
                    Text(
                        text = stringResource(R.string.apply_filters),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
