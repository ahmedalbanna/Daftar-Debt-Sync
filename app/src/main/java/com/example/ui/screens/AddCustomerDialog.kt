package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, notes: String, openingBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var openingBalanceText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var showOpeningBalance by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة زبون جديد", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("اسم الزبون *", style = MaterialTheme.typography.bodyMedium) },
                    isError = nameError,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )
                if (nameError) {
                    Text(
                        text = "يرجى كتابة اسم الزبون",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري)", style = MaterialTheme.typography.bodyMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات / العنوان", style = MaterialTheme.typography.bodyMedium) },
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_notes_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!showOpeningBalance) {
                    TextButton(
                        onClick = { showOpeningBalance = true },
                        modifier = Modifier.testTag("toggle_opening_balance_button")
                    ) {
                        Text(
                            "+ إضافة رصيد سابق (افتتاحي)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "الرصيد الافتتاحي (دين سابق)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "سجل هنا المبلغ الإجمالي للدين السابق المنقول من الدفتر الورقي. سيتم تسجيله كأول حركة للزبون.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = openingBalanceText,
                                onValueChange = { openingBalanceText = it },
                                label = { Text("مبلغ الدين السابق (د.ع)", style = MaterialTheme.typography.bodyMedium) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("opening_balance_input")
                            )
                            TextButton(
                                onClick = {
                                    showOpeningBalance = false
                                    openingBalanceText = ""
                                },
                                modifier = Modifier.testTag("remove_opening_balance_button")
                            ) {
                                Text(
                                    "إزالة الرصيد الافتتاحي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        val openingBalance = openingBalanceText.toDoubleOrNull()
                            ?: 0.0
                        onConfirm(name, phone, notes, openingBalance)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text("حفظ الزبون", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_customer_button")
            ) {
                Text("إلغاء", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
