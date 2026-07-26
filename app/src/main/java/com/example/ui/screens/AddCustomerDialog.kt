package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
    var openingBalance by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

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
                    value = openingBalance,
                    onValueChange = { openingBalance = it },
                    label = { Text("الرصيد الافتتاحي (دين عليه + / له -)", style = MaterialTheme.typography.bodyMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = { Text("0") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_opening_balance_input")
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name, phone, notes, openingBalance.toDoubleOrNull() ?: 0.0)
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
