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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.egamberganov.finflow.ui.theme.appColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
        categoryId: Long,
        accountId: Long,
        dateMillis: Long,
        note: String?,
        attachmentUri: String?
    ) -> Unit,
    onOpenScheduledPayment: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Form State
    var transactionType by remember {
        mutableStateOf(editingTransaction?.transaction?.type ?: "EXPENSE")
    }

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

                // Transfer Tab (Info only)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            Toast.makeText(context, context.getString(R.string.transfer_coming_soon), Toast.LENGTH_SHORT).show()
                        }
                        .testTag("type_tab_transfer"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.type_transfer),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                                color = if (transactionType == "EXPENSE") MaterialTheme.appColors.expense else MaterialTheme.appColors.income,
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
                            Text(
                                text = selectedAccount?.currency ?: "UZS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

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
                    text = if (editingTransaction != null) stringResource(R.string.update_transaction) else stringResource(R.string.save_transaction),
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
