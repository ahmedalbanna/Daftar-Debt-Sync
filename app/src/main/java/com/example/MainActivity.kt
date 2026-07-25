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
            onNetworkStatusClick = { showNetworkStatusDialog = true }
        )
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, notes ->
                viewModel.addCustomer(name, phone, notes)
            }
        )
    }

    if (showAddTransactionDialog && currentCustomer != null) {
        AddTransactionDialog(
            customerName = currentCustomer.customer.name,
            initialType = transactionTypeToAdd,
            onDismiss = { showAddTransactionDialog = false },
            onConfirm = { type, amount, note ->
                viewModel.addTransaction(currentCustomer.customer.id, type, amount, note)
            }
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
