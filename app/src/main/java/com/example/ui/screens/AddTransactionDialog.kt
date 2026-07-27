package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.TransactionType
import com.example.ui.util.SpeechParser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    customerName: String,
    initialType: TransactionType = TransactionType.DEBT,
    existingCustomerNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (type: TransactionType, amount: Double, note: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    val presetAmounts = listOf("5000", "10000", "25000", "50000", "100000")
    val context = LocalContext.current

    // SpeechRecognizer management
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الكلام"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهت مهلة التسجيل"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "المتعرف على الصوت مشغول"
                    else -> "خطأ في التعرف على الصوت (رمز $error)"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches.isNullOrEmpty()) {
                    Toast.makeText(context, "لم يتم التعرف على الكلام", Toast.LENGTH_SHORT).show()
                    return
                }

                val spokenText = matches[0]
                val parseResult = SpeechParser.parse(
                    text = spokenText,
                    existingCustomerNames = existingCustomerNames
                )

                // Apply parsed values
                var updated = false

                if (parseResult.amount != null) {
                    amountText = parseResult.amount.toLong().toString()
                    updated = true
                }

                if (parseResult.type != null) {
                    selectedType = parseResult.type
                    updated = true
                }

                // Build note from description (add to existing if any)
                val descPart = parseResult.description
                if (descPart.isNotBlank()) {
                    note = if (note.isBlank()) descPart else "$note - $descPart"
                    updated = true
                }

                if (!updated) {
                    // If parsing failed, put the raw text in the note
                    note = if (note.isBlank()) spokenText else "$note - $spokenText"
                    Toast.makeText(
                        context,
                        "لم يتم التعرف على المبلغ أو النوع، تم نسخ النص للملاحظات",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
    }

    // Cleanup speech recognizer on dispose
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (isListening) {
                speechRecognizer?.stopListening()
                isListening = false
            }
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedType == TransactionType.DEBT) "تسجيل دين جديد ($customerName)" else "تسجيل دفعة تسديد ($customerName)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                // Microphone button for voice input
                if (speechRecognizer != null) {
                    IconButton(
                        onClick = {
                            if (isListening) {
                                speechRecognizer.stopListening()
                                isListening = false
                            } else {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ")
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
                                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "قل: اسم الزبون + دين/تسديد + المبلغ")
                                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                                }
                                speechRecognizer.setRecognitionListener(recognitionListener)
                                speechRecognizer.startListening(intent)
                                isListening = true
                            }
                        },
                        modifier = Modifier
                            .testTag("voice_input_button")
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "إيقاف التسجيل" else "إدخال صوتي",
                            tint = if (isListening)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isListening) {
                    Text(
                        text = "⏺ جاري الاستماع... تحدث بالعربية: اسم الزبون + نوع الحركة + المبلغ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
                    label = { Text("المبلغ (د.ع) *", style = MaterialTheme.typography.bodyMedium) },
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

                if (speechRecognizer == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الإدخال الصوتي غير متوفر على هذا الجهاز",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            val buttonColor = if (selectedType == TransactionType.DEBT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            val onButtonColor = if (selectedType == TransactionType.DEBT) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = onButtonColor
                ),
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text(
                    text = if (selectedType == TransactionType.DEBT) "حفظ الدين" else "حفظ التسديد",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isListening) {
                    speechRecognizer?.stopListening()
                    isListening = false
                }
                onDismiss()
            }) {
                Text("إلغاء")
            }
        }
    )
}
