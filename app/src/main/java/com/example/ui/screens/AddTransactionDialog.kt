package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.TransactionType

@Composable
fun AddDebtDialog(
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    TransactionDialog(
        title = "تسجيل دين جديد ($customerName)",
        type = TransactionType.DEBT,
        onDismiss = onDismiss,
        onConfirm = { _, amount, note -> onConfirm(amount, note) }
    )
}

@Composable
fun AddPaymentDialog(
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    TransactionDialog(
        title = "تسجيل دفعة تسديد ($customerName)",
        type = TransactionType.PAYMENT,
        onDismiss = onDismiss,
        onConfirm = { _, amount, note -> onConfirm(amount, note) }
    )
}

@Composable
fun EditTransactionDialog(
    type: TransactionType,
    initialAmount: Double,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (type: TransactionType, amount: Double, note: String) -> Unit,
    onDelete: () -> Unit
) {
    TransactionDialog(
        title = "تعديل حركة",
        type = type,
        initialAmount = initialAmount.toString(),
        initialNote = initialNote,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onDelete = onDelete
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionDialog(
    title: String,
    type: TransactionType,
    initialAmount: String = "",
    initialNote: String = "",
    onDismiss: () -> Unit,
    onConfirm: (type: TransactionType, amount: Double, note: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var amountText by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf(initialNote) }
    var amountError by remember { mutableStateOf(false) }

    val presetAmounts = listOf("5000", "10000", "25000", "50000", "100000")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (it.isNotBlank() && it.toDoubleOrNull() != null) amountError = false
                    },
                    label = { Text("المبلغ (ر.ي) *", style = MaterialTheme.typography.bodyMedium) },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input")
                )
                if (amountError) {
                    Text(
                        text = "يرجى كتابة مبلغ صحيح",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("مبالغ سريعة:", style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        FilterChip(
                            selected = amountText == preset,
                            onClick = {
                                amountText = preset
                                amountError = false
                            },
                            label = { Text(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة / وصف البضاعة (اختياري)", style = MaterialTheme.typography.bodyMedium) },
                    maxLines = 2,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input")
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف", style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء", style = MaterialTheme.typography.labelLarge)
                }
                
                val isDebt = type == TransactionType.DEBT
                val buttonColor = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                val onButtonColor = if (isDebt) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            amountError = true
                        } else {
                            onConfirm(type, amount, note)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = onButtonColor
                    ),
                    modifier = Modifier.testTag("save_transaction_button")
                ) {
                    Text(
                        text = "حفظ",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        dismissButton = {}
    )
}
