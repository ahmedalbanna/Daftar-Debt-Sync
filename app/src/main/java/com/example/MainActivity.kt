package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.TransactionType
import com.example.ui.screens.AddCustomerDialog
import com.example.ui.screens.AddTransactionDialog
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.ImportBalancesDialog
import com.example.ui.screens.ImportFromContactsDialog
import com.example.ui.screens.MainLedgerScreen
import com.example.ui.screens.NetworkStatusDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                DaftarApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DaftarApp(viewModel: LedgerViewModel) {
    val customers by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val totalShopDebt by viewModel.totalShopDebt.collectAsStateWithLifecycle()
    val selectedCustomerDetail by viewModel.selectedCustomerDetail.collectAsStateWithLifecycle()
    val selectedCustomerTransactions by viewModel.selectedCustomerTransactions.collectAsStateWithLifecycle()

    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var transactionTypeToAdd by remember { mutableStateOf(TransactionType.DEBT) }
    var showNetworkStatusDialog by remember { mutableStateOf(false) }
    var showImportBalancesDialog by remember { mutableStateOf(false) }
    var showImportContactsDialog by remember { mutableStateOf(false) }

    val currentCustomer = selectedCustomerDetail

    if (currentCustomer != null) {
        CustomerDetailScreen(
            customerWithBalance = currentCustomer,
            transactions = selectedCustomerTransactions,
            onBackClick = { viewModel.selectCustomer(null) },
            onAddTransactionClick = { type ->
                transactionTypeToAdd = type
                showAddTransactionDialog = true
            }
        )
    } else {
        MainLedgerScreen(
            customers = customers,
            searchQuery = searchQuery,
            totalShopDebt = totalShopDebt,
            discoveredPeers = discoveredPeers,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onCustomerClick = { customerId -> viewModel.selectCustomer(customerId) },
            onAddCustomerClick = { showAddCustomerDialog = true },
            onImportBalancesClick = { showImportBalancesDialog = true },
            onImportContactsClick = { showImportContactsDialog = true },
            onNetworkStatusClick = { showNetworkStatusDialog = true }
        )
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, notes, openingBalance ->
                if (openingBalance > 0) {
                    viewModel.addCustomerWithOpeningBalance(name, phone, notes, openingBalance)
                } else {
                    viewModel.addCustomer(name, phone, notes)
                }
            }
        )
    }

    if (showAddTransactionDialog && currentCustomer != null) {
        val existingNames = customers.map { it.customer.name }
        AddTransactionDialog(
            customerName = currentCustomer.customer.name,
            initialType = transactionTypeToAdd,
            existingCustomerNames = existingNames,
            onDismiss = { showAddTransactionDialog = false },
            onConfirm = { type, amount, note ->
                viewModel.addTransaction(currentCustomer.customer.id, type, amount, note)
            }
        )
    }

    if (showImportBalancesDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ImportBalancesDialog(
            onImport = { uri -> viewModel.importFromFile(context, uri) },
            onDismiss = { showImportBalancesDialog = false }
        )
    }

    if (showImportContactsDialog) {
        ImportFromContactsDialog(
            onImport = { contacts -> viewModel.importContacts(contacts) },
            onDismiss = { showImportContactsDialog = false }
        )
    }

    if (showNetworkStatusDialog) {
        NetworkStatusDialog(
            localIp = viewModel.syncOrchestrator.localIp,
            deviceId = viewModel.deviceId,
            peers = discoveredPeers,
            logs = syncLogs,
            isSyncing = isSyncing,
            onTriggerSync = { viewModel.triggerManualSync() },
            onExportBackup = { viewModel.exportBackupJson() },
            onImportBackup = { json -> viewModel.importBackupJson(json) },
            onDismiss = { showNetworkStatusDialog = false }
        )
    }
}
