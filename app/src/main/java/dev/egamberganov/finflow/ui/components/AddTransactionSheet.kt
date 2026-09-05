package dev.egamberganov.finflow.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.domain.model.AccountWithBalance
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.ui.theme.appColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountWithBalance>,
    selectedAccountId: Long?,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    editingTransaction: TransactionWithDetails? = null,
    onSaveTransaction: (
        type: String,
        amount: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long,
        dateMillis: Long,
        note: String?,
        attachmentUri: String?
    ) -> Unit,
    onSaveTransfer: (
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        dateMillis: Long,
        note: String?,
        attachmentUri: String?,
        exchangeRate: Double?,
        convertedAmount: Long?
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onFetchExchangeRate: (suspend (fromCurrency: String, toCurrency: String) -> Result<Double>)? = null,
    onOpenScheduledPayment: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Form State
    var transactionType by remember {
        mutableStateOf(editingTransaction?.transaction?.type ?: "EXPENSE")
    }

    val isTransfer = transactionType == "TRANSFER"

    var amountText by remember {
        mutableStateOf(editingTransaction?.transaction?.amount?.toString() ?: "")
    }

    val currentCategories = if (transactionType == "EXPENSE") expenseCategories else incomeCategories

    var selectedCategoryId by remember {
        mutableLongStateOf(
            editingTransaction?.transaction?.categoryId
                ?: currentCategories.firstOrNull()?.id
                ?: 1L
        )
    }

    // Default account to context account or first available
    val initialAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
    var selectedAccount by remember {
        mutableStateOf(
            if (editingTransaction != null) {
                accounts.find { it.id == editingTransaction.transaction.accountId } ?: initialAccount
            } else {
                initialAccount
            }
        )
    }

    // Transfer specific accounts & conversion state
    var fromAccount by remember {
        mutableStateOf(
            if (editingTransaction != null && editingTransaction.transaction.type == "TRANSFER") {
                accounts.find { it.id == editingTransaction.transaction.accountId } ?: initialAccount
            } else {
                initialAccount
            }
        )
    }

    var toAccount by remember {
        mutableStateOf(
            if (editingTransaction != null && editingTransaction.transaction.type == "TRANSFER" && editingTransaction.transaction.toAccountId != null) {
                accounts.find { it.id == editingTransaction.transaction.toAccountId }
            } else {
                accounts.firstOrNull { it.id != fromAccount?.id }
            }
        )
    }

    var exchangeRate by remember {
        mutableStateOf<Double?>(editingTransaction?.transaction?.exchangeRate)
    }

    var manualRateText by remember {
        mutableStateOf(editingTransaction?.transaction?.exchangeRate?.let { if (it > 0) it.toString() else "" } ?: "")
    }

    var isFetchingRate by remember { mutableStateOf(false) }
    var rateFetchError by remember { mutableStateOf<String?>(null) }

    var showFromAccountPickerDropdown by remember { mutableStateOf(false) }
    var showToAccountPickerDropdown by remember { mutableStateOf(false) }

    val isDifferentCurrency = fromAccount != null && toAccount != null &&
            !fromAccount!!.currency.equals(toAccount!!.currency, ignoreCase = true)

    LaunchedEffect(isTransfer, fromAccount?.currency, toAccount?.currency) {
        if (isTransfer && isDifferentCurrency && fromAccount != null && toAccount != null) {
            if (exchangeRate == null || editingTransaction == null) {
                isFetchingRate = true
                rateFetchError = null
                if (onFetchExchangeRate != null) {
                    val res = onFetchExchangeRate(fromAccount!!.currency, toAccount!!.currency)
                    res.fold(
                        onSuccess = { rate ->
                            exchangeRate = rate
                            manualRateText = rate.toString()
                            isFetchingRate = false
                        },
                        onFailure = { err ->
                            rateFetchError = err.message ?: context.getString(R.string.exchange_rate_error)
                            isFetchingRate = false
                        }
                    )
                } else {
                    isFetchingRate = false
                }
            }
        }
    }

    var selectedDateMillis by remember {
        mutableLongStateOf(editingTransaction?.transaction?.dateMillis ?: System.currentTimeMillis())
    }

    var noteText by remember {
        mutableStateOf(editingTransaction?.transaction?.note ?: "")
    }

    var attachmentUri by remember {
        mutableStateOf(editingTransaction?.transaction?.attachmentUri)
    }

    var showAccountPickerDropdown by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Date & Time Formatting
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) stringResource(R.string.edit_transaction) else stringResource(R.string.add_transaction),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("add_tx_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selection Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expense Tab
                val isExpense = transactionType == "EXPENSE"
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            transactionType = "EXPENSE"
                            if (expenseCategories.isNotEmpty() && expenseCategories.none { it.id == selectedCategoryId }) {
                                selectedCategoryId = expenseCategories.first().id
                            }
                        }
                        .testTag("type_tab_expense"),
                    color = if (isExpense) MaterialTheme.appColors.expense else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.type_expense),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                // Income Tab
                val isIncome = transactionType == "INCOME"
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            transactionType = "INCOME"
                            if (incomeCategories.isNotEmpty() && incomeCategories.none { it.id == selectedCategoryId }) {
                                selectedCategoryId = incomeCategories.first().id
                            }
                        }
                        .testTag("type_tab_income"),
                    color = if (isIncome) MaterialTheme.appColors.income else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.type_income),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                        color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                // Transfer Tab
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            transactionType = "TRANSFER"
                        }
                        .testTag("type_tab_transfer"),
                    color = if (isTransfer) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.type_transfer),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isTransfer) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTransfer) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                // Scheduled Tab
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            onDismiss()
                            onOpenScheduledPayment()
                        }
                        .testTag("type_tab_scheduled"),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.appColors.brand,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.scheduled_payments),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.appColors.brand
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Input Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.amount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    amountText = input
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transaction_amount_input"),
                            textStyle = MaterialTheme.typography.displayMedium.copy(
                                color = when {
                                    isTransfer -> MaterialTheme.appColors.brand
                                    transactionType == "EXPENSE" -> MaterialTheme.appColors.expense
                                    else -> MaterialTheme.appColors.income
                                },
                                fontWeight = FontWeight.Bold
                            ),
                            placeholder = {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            val displayCurrency = if (isTransfer) {
                                fromAccount?.currency ?: "UZS"
                            } else {
                                selectedAccount?.currency ?: "UZS"
                            }
                            Text(
                                text = displayCurrency,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (isTransfer && fromAccount != null) {
                        val amountLong = amountText.toLongOrNull() ?: 0L
                        val isInsufficient = amountLong > fromAccount!!.currentBalance
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.source_balance, CurrencyFormatter.formatAmount(fromAccount!!.currentBalance, fromAccount!!.currency)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isInsufficient) MaterialTheme.appColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isInsufficient) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isInsufficient) {
                            Text(
                                text = stringResource(R.string.transfer_insufficient_funds),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.appColors.expense
                            )
                        }
                    }
                }
            }

            if (!isTransfer) {
                Spacer(modifier = Modifier.height(20.dp))

                // Category Selection
                Text(
                    text = stringResource(R.string.select_category).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    currentCategories.forEach { category ->
                        val isSelected = selectedCategoryId == category.id
                        val catColor = CategoryVisuals.parseColor(category.colorHex)

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategoryId = category.id }
                                .padding(4.dp)
                                .testTag("category_select_${category.id}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = if (isSelected) 1f else 0.15f))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.appColors.brand, CircleShape)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = CategoryVisuals.getDrawableForName(category.name.ifEmpty { category.iconName })),
                                    contentDescription = category.name,
                                    tint = if (isSelected) Color.White else catColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Account Selection Card
                Text(
                    text = stringResource(R.string.account).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAccountPickerDropdown = !showAccountPickerDropdown }
                        .testTag("transaction_account_selector"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountTypeBadge(
                            accountType = selectedAccount?.type ?: "Cash",
                            colorHex = selectedAccount?.colorHex ?: "#6C5CE7",
                            size = 38.dp,
                            iconSize = 20.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedAccount?.name ?: "Cash Wallet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedAccount?.type ?: "Cash"} · ${selectedAccount?.currency ?: "UZS"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.appColors.brand.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = stringResource(R.string.auto_selected),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.appColors.brand,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Expanded Account Selection Choices
                if (showAccountPickerDropdown) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        accounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedAccount = acc
                                        showAccountPickerDropdown = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${acc.name} (${acc.currency})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedAccount?.id == acc.id) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.appColors.brand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // TRANSFER MODE: From and To Account Pickers
                Spacer(modifier = Modifier.height(20.dp))

                // FROM ACCOUNT
                Text(
                    text = stringResource(R.string.transfer_from_account).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showFromAccountPickerDropdown = !showFromAccountPickerDropdown }
                        .testTag("transfer_from_account_selector"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountTypeBadge(
                            accountType = fromAccount?.type ?: "Cash",
                            colorHex = fromAccount?.colorHex ?: "#6C5CE7",
                            size = 38.dp,
                            iconSize = 20.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fromAccount?.name ?: "Select Source Account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${fromAccount?.type ?: ""} · ${CurrencyFormatter.formatAmount(fromAccount?.currentBalance ?: 0L, fromAccount?.currency ?: "")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showFromAccountPickerDropdown) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        accounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        fromAccount = acc
                                        showFromAccountPickerDropdown = false
                                        if (toAccount?.id == acc.id) {
                                            toAccount = accounts.firstOrNull { it.id != acc.id }
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${acc.name} (${acc.currency}) · ${CurrencyFormatter.formatAmount(acc.currentBalance, acc.currency)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (fromAccount?.id == acc.id) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.appColors.brand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Direction Icon
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.appColors.brand.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tx_transfer),
                                contentDescription = null,
                                tint = MaterialTheme.appColors.brand,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TO ACCOUNT
                Text(
                    text = stringResource(R.string.transfer_to_account).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showToAccountPickerDropdown = !showToAccountPickerDropdown }
                        .testTag("transfer_to_account_selector"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountTypeBadge(
                            accountType = toAccount?.type ?: "Cash",
                            colorHex = toAccount?.colorHex ?: "#6C5CE7",
                            size = 38.dp,
                            iconSize = 20.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = toAccount?.name ?: "Select Destination Account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${toAccount?.type ?: ""} · ${CurrencyFormatter.formatAmount(toAccount?.currentBalance ?: 0L, toAccount?.currency ?: "")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showToAccountPickerDropdown) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        accounts.forEach { acc ->
                            val isSameAsFrom = acc.id == fromAccount?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isSameAsFrom) {
                                            Toast.makeText(context, context.getString(R.string.transfer_same_account_error), Toast.LENGTH_SHORT).show()
                                        } else {
                                            toAccount = acc
                                            showToAccountPickerDropdown = false
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${acc.name} (${acc.currency}) · ${CurrencyFormatter.formatAmount(acc.currentBalance, acc.currency)}" +
                                            if (isSameAsFrom) " (${stringResource(R.string.transfer_from_account)})" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSameAsFrom) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (toAccount?.id == acc.id) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.appColors.brand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Currency Conversion Card (if different currencies)
                if (isDifferentCurrency && fromAccount != null && toAccount != null) {
                    val amountLong = amountText.toLongOrNull() ?: 0L
                    val effectiveRate = manualRateText.toDoubleOrNull() ?: exchangeRate
                    val convertedAmount: Long? = if (effectiveRate != null && effectiveRate > 0.0) {
                        (amountLong * effectiveRate).roundToLong()
                    } else null

                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.appColors.brand.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.appColors.brand.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_conversion),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isFetchingRate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.appColors.brand
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isFetchingRate = true
                                                rateFetchError = null
                                                if (onFetchExchangeRate != null) {
                                                    val res = onFetchExchangeRate(fromAccount!!.currency, toAccount!!.currency)
                                                    res.fold(
                                                        onSuccess = { rate ->
                                                            exchangeRate = rate
                                                            manualRateText = rate.toString()
                                                            isFetchingRate = false
                                                        },
                                                        onFailure = { err ->
                                                            rateFetchError = err.message ?: context.getString(R.string.exchange_rate_error)
                                                            isFetchingRate = false
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_tx_recurring),
                                            contentDescription = "Refresh Rate",
                                            tint = MaterialTheme.appColors.brand,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Rate Row with Editable Field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1 ${fromAccount!!.currency} = ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = manualRateText,
                                    onValueChange = { manualRateText = it },
                                    modifier = Modifier
                                        .width(120.dp)
                                        .testTag("transfer_rate_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.appColors.brand,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = toAccount!!.currency,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (rateFetchError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rateFetchError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.appColors.expense
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Converted Amount Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.destination_receives),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatAmount(convertedAmount ?: 0L, toAccount!!.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.appColors.brand
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Date & Time Picker Card
            Text(
                text = stringResource(R.string.date_and_time).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        showDatePickerDialog = true
                    }
                    .testTag("transaction_date_picker"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateFormatter.format(Date(selectedDateMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tx_recurring),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Note (Optional)
            Text(
                text = stringResource(R.string.note_optional).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.note_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    focusedBorderColor = MaterialTheme.appColors.brand
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Attachment (Optional)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        attachmentUri = if (attachmentUri == null) "receipt_sample_attachment_${System.currentTimeMillis()}" else null
                    }
                    .testTag("transaction_attachment_button"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_act_attach_receipt),
                        contentDescription = null,
                        tint = if (attachmentUri != null) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (attachmentUri != null) stringResource(R.string.attachment_added) else stringResource(R.string.add_attachment),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (attachmentUri != null) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Transaction Button
            Button(
                onClick = {
                    val amountLong = amountText.toLongOrNull() ?: 0L
                    if (amountLong <= 0L) {
                        Toast.makeText(context, context.getString(R.string.invalid_amount_error), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (isTransfer) {
                        val fromAcc = fromAccount
                        val toAcc = toAccount
                        if (fromAcc == null || toAcc == null) {
                            Toast.makeText(context, context.getString(R.string.select_account_error), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (fromAcc.id == toAcc.id) {
                            Toast.makeText(context, context.getString(R.string.transfer_same_account_error), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amountLong > fromAcc.currentBalance) {
                            Toast.makeText(context, context.getString(R.string.transfer_insufficient_funds), Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val isDiff = !fromAcc.currency.equals(toAcc.currency, ignoreCase = true)
                        val rate = manualRateText.toDoubleOrNull() ?: exchangeRate
                        val conv = if (isDiff) {
                            if (rate == null || rate <= 0.0) {
                                Toast.makeText(context, context.getString(R.string.exchange_rate_error), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            (amountLong * rate).roundToLong()
                        } else {
                            amountLong
                        }

                        onSaveTransfer(
                            fromAcc.id,
                            toAcc.id,
                            amountLong,
                            selectedDateMillis,
                            noteText,
                            attachmentUri,
                            if (isDiff) rate else null,
                            conv
                        )
                    } else {
                        val acc = selectedAccount
                        if (acc == null) {
                            Toast.makeText(context, context.getString(R.string.select_account_error), Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        onSaveTransaction(
                            transactionType,
                            amountLong,
                            acc.currency,
                            selectedCategoryId,
                            acc.id,
                            selectedDateMillis,
                            noteText,
                            attachmentUri
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.appColors.brand
                )
            ) {
                Text(
                    text = when {
                        isTransfer -> if (editingTransaction != null) stringResource(R.string.edit_transfer) else stringResource(R.string.save_transfer)
                        editingTransaction != null -> stringResource(R.string.update_transaction)
                        else -> stringResource(R.string.save_transaction)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedUtcMillis = datePickerState.selectedDateMillis
                        if (pickedUtcMillis != null) {
                            val pickedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = pickedUtcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                timeInMillis = selectedDateMillis
                                set(Calendar.YEAR, pickedCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, pickedCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, pickedCal.get(Calendar.DAY_OF_MONTH))
                            }
                            selectedDateMillis = localCal.timeInMillis
                        }
                        showDatePickerDialog = false
                        showTimePickerDialog = true
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePickerDialog) {
        val timeCal = remember(selectedDateMillis) {
            Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = timeCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = timeCal.get(Calendar.MINUTE),
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        selectedDateMillis = updatedCal.timeInMillis
                        showTimePickerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.select_time),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}
