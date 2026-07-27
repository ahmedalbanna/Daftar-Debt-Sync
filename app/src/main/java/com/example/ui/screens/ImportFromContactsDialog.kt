package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.imports.ContactInfo
import com.example.data.imports.ContactsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImportFromContactsDialog(
    onImport: suspend (List<ContactInfo>) -> Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissionGranted by remember { mutableStateOf(false) }
    var isLoadingContacts by remember { mutableStateOf(false) }
    var hasReadContacts by remember { mutableStateOf(false) }
    val contacts = remember { mutableStateListOf<ContactInfo>() }
    val selectedContacts = remember { mutableStateListOf<ContactInfo>() }
    var searchQuery by remember { mutableStateOf("") }
    var importedCount by remember { mutableStateOf(0) }
    var isImporting by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            hasReadContacts = true
            loadContactsAsync(context, contacts) { isLoadingContacts = it }
        }
    }

    // Filter contacts based on search
    val filteredContacts = if (searchQuery.isBlank()) {
        contacts.toList()
    } else {
        val q = searchQuery.trim().lowercase()
        contacts.filter {
            it.name.lowercase().contains(q) || it.phone.contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("استيراد من جهات الاتصال")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!permissionGranted && !hasReadContacts) {
                    Text(
                        text = "يحتاج التطبيق إذن الوصول إلى جهات الاتصال لاستيراد أسماء وأرقام الزبائن.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        modifier = Modifier.testTag("request_contacts_permission_button")
                    ) {
                        Text("منح الإذن للوصول إلى جهات الاتصال")
                    }
                } else if (isLoadingContacts) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("جاري تحميل جهات الاتصال...")
                    }
                } else if (contacts.isEmpty() && hasReadContacts) {
                    Text(
                        text = "لم يتم العثور على جهات اتصال بأرقام هواتف.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (contacts.isNotEmpty()) {
                    // Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("بحث باسم الزبون") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contacts_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${contacts.size} جهة اتصال - تم اختيار ${selectedContacts.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Select/Deselect All
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                selectedContacts.clear()
                                selectedContacts.addAll(filteredContacts)
                            },
                            modifier = Modifier.testTag("select_all_contacts_button")
                        ) {
                            Text("تحديد الكل", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(
                            onClick = { selectedContacts.clear() },
                            modifier = Modifier.testTag("deselect_all_contacts_button")
                        ) {
                            Text("إلغاء التحديد", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Contacts list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(filteredContacts, key = { it.lookupKey }) { contact ->
                            val isSelected = contact in selectedContacts
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (contact !in selectedContacts) {
                                                    selectedContacts.add(contact)
                                                }
                                            } else {
                                                selectedContacts.remove(contact)
                                            }
                                        },
                                        modifier = Modifier.testTag("contact_checkbox_${contact.lookupKey.take(8)}")
                                    )
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = contact.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Import button
                    Button(
                        onClick = {
                            scope.launch {
                                isImporting = true
                                val count = withContext(Dispatchers.IO) {
                                    onImport(selectedContacts.toList())
                                }
                                importedCount = count
                                isImporting = false
                                if (importedCount > 0) {
                                    Toast.makeText(
                                        context,
                                        "تم استيراد $importedCount زبون بنجاح!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_contacts_button"),
                        enabled = selectedContacts.isNotEmpty() && !isImporting
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استيراد (${selectedContacts.size}) زبون")
                        }
                    }

                    if (importedCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تم استيراد $importedCount زبون بنجاح",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_contacts_import_button")
            ) {
                Text(if (importedCount > 0) "تم" else "إغلاق")
            }
        },
        dismissButton = {
            if (!isImporting) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_contacts_import_button")
                ) {
                    Text("إلغاء")
                }
            }
        }
    )
}

/**
 * Load contacts on a background thread using a callback for loading state.
 */
private fun loadContactsAsync(
    context: android.content.Context,
    contacts: MutableList<ContactInfo>,
    onLoadingChanged: (Boolean) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        onLoadingChanged(true)
        val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) {
            ContactsReader.readContacts(context)
        }
        contacts.clear()
        contacts.addAll(loaded)
        onLoadingChanged(false)
    }
}
