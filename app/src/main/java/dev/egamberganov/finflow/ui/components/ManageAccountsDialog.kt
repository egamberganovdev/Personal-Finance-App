package dev.egamberganov.finflow.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.domain.model.AccountType
import dev.egamberganov.finflow.domain.model.AccountWithBalance
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.ui.theme.appColors

@Composable
fun ManageAccountsDialog(
    accounts: List<AccountWithBalance>,
    onAddAccount: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var accountCannotDelete by remember { mutableStateOf<AccountWithBalance?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.manage_accounts),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("manage_accounts_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Accounts List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AccountTypeBadge(
                                    accountType = acc.type,
                                    colorHex = acc.colorHex,
                                    size = 40.dp,
                                    iconSize = 20.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val typeLabel = stringResource(acc.accountType.stringResId)
                                    Text(
                                        text = "$typeLabel · ${acc.currency} · ${CurrencyFormatter.formatAmount(acc.currentBalance, acc.currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Edit Button
                                IconButton(
                                    onClick = { onEditAccount(acc.account) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("edit_account_button_${acc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.action_edit),
                                        tint = MaterialTheme.appColors.brand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete Button
                                IconButton(
                                    onClick = {
                                        if (accounts.size <= 1) {
                                            Toast.makeText(context, context.getString(R.string.cannot_delete_last_account), Toast.LENGTH_SHORT).show()
                                        } else if (acc.transactionCount > 0) {
                                            accountCannotDelete = acc
                                        } else {
                                            accountToDelete = acc.account
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_account_button_${acc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.appColors.expense,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Add Account Button
                Button(
                    onClick = onAddAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("manage_accounts_add_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.brand)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_account),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Account Cannot Delete Dialog (Historical transactions protect safeguard)
    if (accountCannotDelete != null) {
        AlertDialog(
            onDismissRequest = { accountCannotDelete = null },
            title = { Text(stringResource(R.string.cannot_delete_account_title)) },
            text = { Text(stringResource(R.string.cannot_delete_account_has_transactions)) },
            confirmButton = {
                TextButton(
                    onClick = { accountCannotDelete = null },
                    modifier = Modifier.testTag("dismiss_cannot_delete_account_button")
                ) {
                    Text(stringResource(R.string.action_ok), color = MaterialTheme.appColors.brand)
                }
            }
        )
    }

    // Delete Confirmation Dialog (Only for accounts with zero transactions)
    if (accountToDelete != null) {
        val target = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.delete_account_confirm, target.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(target)
                        accountToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.appColors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun CreateAccountDialog(
    editingAccount: AccountEntity? = null,
    onSave: (name: String, type: String, currency: String, initialBalance: Long, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editingAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(AccountType.fromString(editingAccount?.type)) }
    var currency by remember { mutableStateOf(editingAccount?.currency ?: "UZS") }
    var initialBalanceText by remember { mutableStateOf(editingAccount?.initialBalance?.toString() ?: "0") }
    var selectedColorHex by remember { mutableStateOf(editingAccount?.colorHex ?: "#6C5CE7") }

    val currencies = listOf("UZS", "USD", "EUR", "RUB", "GBP", "KZT")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingAccount != null) stringResource(R.string.edit_account) else stringResource(R.string.add_account),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Name
                Text(
                    text = stringResource(R.string.account_name),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    placeholder = { Text(stringResource(R.string.account_name_placeholder)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.appColors.brand
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Account Type Selector (6 types supported)
                Text(
                    text = stringResource(R.string.account_type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.entries.chunked(3).forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTypes.forEach { accType ->
                                val isSel = selectedType == accType
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedType = accType }
                                        .testTag("account_type_choice_${accType.dbKey}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) MaterialTheme.appColors.brand.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSel) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = accType.iconResId),
                                            contentDescription = null,
                                            tint = if (isSel) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(accType.stringResId),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Currency Selector
                Text(
                    text = stringResource(R.string.currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (editingAccount != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_currency_locked_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currencies.take(4).forEach { curr ->
                            val isSel = currency == curr
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { currency = curr }
                                    .testTag("currency_choice_$curr"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (!isSel) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = curr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Initial Balance
                Text(
                    text = stringResource(R.string.initial_balance),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) initialBalanceText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.appColors.brand
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Color Picker
                Text(
                    text = stringResource(R.string.category_color),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryVisuals.availableColors.take(6).forEach { hex ->
                        val isColorSel = selectedColorHex.equals(hex, ignoreCase = true)
                        val c = CategoryVisuals.parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColorHex = hex }
                                .then(
                                    if (isColorSel) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val initBal = initialBalanceText.toLongOrNull() ?: 0L
                        if (name.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.enter_account_name_error), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSave(name.trim(), selectedType.dbKey, currency, initBal, selectedColorHex)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_account_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.brand)
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
