package dev.egamberganov.finflow.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.ui.theme.appColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    item: TransactionWithDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val isExpense = item.transaction.type == "EXPENSE"
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy · HH:mm", Locale.getDefault()) }
    val itemColor = if (isExpense) MaterialTheme.appColors.expense else MaterialTheme.appColors.income

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
                    .padding(24.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.transaction_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tx_detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Amount Display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CategoryIconBadge(
                        iconName = item.category?.iconName ?: "other",
                        colorHex = item.category?.colorHex ?: "#6C5CE7",
                        categoryName = item.category?.name ?: "Other",
                        size = 56.dp,
                        iconSize = 28.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = CurrencyFormatter.formatAmount(
                            amount = item.transaction.amount,
                            currency = item.transaction.currency,
                            showSign = true,
                            isExpense = isExpense
                        ),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = itemColor
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = itemColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = if (isExpense) stringResource(R.string.type_expense) else stringResource(R.string.type_income),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = itemColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                // Detail Rows
                // Category
                DetailInfoRow(
                    drawableRes = CategoryVisuals.getDrawableForName(item.category?.name ?: item.category?.iconName ?: "other"),
                    label = stringResource(R.string.categories),
                    value = item.category?.name ?: "Unknown"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Account
                DetailInfoRow(
                    drawableRes = CategoryVisuals.getAccountDrawable(item.account?.type ?: "Cash"),
                    label = stringResource(R.string.account),
                    value = "${item.account?.name ?: "Cash"} (${item.account?.currency ?: "UZS"})"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date & Time
                DetailInfoRow(
                    drawableRes = R.drawable.ic_nav_calendar,
                    label = stringResource(R.string.date_and_time),
                    value = dateFormatter.format(Date(item.transaction.dateMillis))
                )

                // Note
                if (!item.transaction.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailInfoRow(
                        drawableRes = R.drawable.ic_act_attach_receipt,
                        label = stringResource(R.string.note_optional),
                        value = item.transaction.note
                    )
                }

                // Attachment
                if (item.transaction.attachmentUri != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailInfoRow(
                        drawableRes = R.drawable.ic_act_attach_receipt,
                        label = stringResource(R.string.attachment_optional),
                        value = stringResource(R.string.attachment_added)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Edit & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_detail_delete_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.appColors.expense
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.appColors.expense.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_act_delete),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_delete))
                    }

                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_detail_edit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.brand
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_act_edit),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_edit),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_tx_button")
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.appColors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(
    @DrawableRes drawableRes: Int,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
