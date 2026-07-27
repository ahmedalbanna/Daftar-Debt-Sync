package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.imports.ImportRow
import com.example.data.imports.ImportResult
import kotlinx.coroutines.launch

@Composable
fun ImportBalancesDialog(
    onImport: suspend (Uri) -> ImportResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importedCount by remember { mutableIntStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            // Try to extract file name
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        selectedFileName = it.getString(nameIndex)
                    }
                }
            }
            if (selectedFileName == null) {
                selectedFileName = uri.lastPathSegment
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("استيراد زبائن وديون من ملف")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ارفع ملف CSV أو Excel يحتوي على قائمة الزبائن والديون السابقة. " +
                            "يفضل أن يكون التنسيق: الاسم، رقم الهاتف، إجمالي الدين.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File picker button
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_file_button"),
                    enabled = !isImporting
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedFileName != null) selectedFileName!! else "اختيار ملف CSV أو Excel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (selectedFileUri != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isImporting = true
                                importResult = onImport(selectedFileUri!!)
                                importedCount = importResult?.rows?.size ?: 0
                                isImporting = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_import_button"),
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الاستيراد...")
                        } else {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بدء استيراد البيانات")
                        }
                    }
                }

                // Show results after import
                if (importResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (importedCount > 0)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (importedCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تم استيراد $importedCount زبون بنجاح!",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }

                            if (importResult!!.errors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "الأسطر التي لم يتم استيرادها: ${importResult!!.errors.size}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                // Show up to 5 error examples
                                LazyColumn(modifier = Modifier.height(100.dp)) {
                                    items(importResult!!.errors.take(5)) { error ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = error,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_import_button")
            ) {
                Text(if (importedCount > 0) "تم" else "إغلاق")
            }
        },
        dismissButton = {
            if (!isImporting) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_import_button")
                ) {
                    Text("إلغاء")
                }
            }
        }
    )
}
