package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    customerName: String,
    initialType: TransactionType = TransactionType.DEBT,
    onDismiss: () -> Unit,
    onConfirm: (type: TransactionType, amount: Double, note: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val presetAmounts = listOf("5000", "10000", "25000", "50000", "100000")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (selectedType == TransactionType.DEBT) "تسجيل دين جديد ($customerName)" else "تسجيل دفعة تسديد ($customerName)",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == TransactionType.DEBT,
                        onClick = { selectedType = TransactionType.DEBT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.errorContainer,
                            activeContentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("type_debt_button")
                    ) {
                        Text("دين (على الزبون)")
                    }

                    SegmentedButton(
                        selected = selectedType == TransactionType.PAYMENT,
                        onClick = { selectedType = TransactionType.PAYMENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.testTag("type_payment_button")
                    ) {
                        Text("تسديد (من الزبون)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (it.isNotBlank() && it.toDoubleOrNull() != null) amountError = false
                    },
                    label = { Text("المبلغ (د.ع) *") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
                    label = { Text("ملاحظة / وصف البضاعة (اختياري)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input")
                )
            }
        },
        confirmButton = {
            val buttonColor = if (selectedType == TransactionType.DEBT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = true
                    } else {
                        onConfirm(selectedType, amount, note)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text(if (selectedType == TransactionType.DEBT) "حفظ الدين" else "حفظ التسديد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
