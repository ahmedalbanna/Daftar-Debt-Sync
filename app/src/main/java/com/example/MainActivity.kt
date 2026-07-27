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
import com.example.ui.screens.AddDebtDialog
import com.example.ui.screens.AddPaymentDialog
import com.example.ui.screens.EditTransactionDialog
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.MainLedgerScreen
import com.example.ui.screens.NetworkStatusDialog
import com.example.ui.theme.DaftarTheme
import com.example.ui.viewmodel.LedgerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DaftarTheme {
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
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<com.example.data.entity.TransactionEntity?>(null) }
    var showNetworkStatusDialog by remember { mutableStateOf(false) }

    val currentCustomer = selectedCustomerDetail

    if (currentCustomer != null) {
        CustomerDetailScreen(
            customerWithBalance = currentCustomer,
            transactions = selectedCustomerTransactions,
            onBackClick = { viewModel.selectCustomer(null) },
            onAddTransactionClick = { type ->
                if (type == TransactionType.DEBT) {
                    showAddDebtDialog = true
                } else {
                    showAddPaymentDialog = true
                }
            },
            onEditTransactionClick = { tx ->
                transactionToEdit = tx
                showEditTransactionDialog = true
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
            onNetworkStatusClick = { showNetworkStatusDialog = true },
            onBackupClick = { showBackupDialog = true }
        )
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, notes, openingBalance ->
                viewModel.addCustomer(name, phone, notes, openingBalance)
            }
        )
    }

    if (showAddDebtDialog && currentCustomer != null) {
        AddDebtDialog(
            customerName = currentCustomer.customer.name,
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { amount, note ->
                viewModel.addTransaction(currentCustomer.customer.id, TransactionType.DEBT, amount, note)
            }
        )
    }

    if (showAddPaymentDialog && currentCustomer != null) {
        AddPaymentDialog(
            customerName = currentCustomer.customer.name,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, note ->
                viewModel.addTransaction(currentCustomer.customer.id, TransactionType.PAYMENT, amount, note)
            }
        )
    }

    if (showEditTransactionDialog && transactionToEdit != null) {
        EditTransactionDialog(
            type = transactionToEdit!!.type,
            initialAmount = transactionToEdit!!.amount,
            initialNote = transactionToEdit!!.note,
            onDismiss = { 
                showEditTransactionDialog = false
                transactionToEdit = null
            },
            onConfirm = { type, amount, note ->
                val toEdit = transactionToEdit
                if (toEdit != null) {
                    viewModel.updateTransaction(toEdit, amount, note)
                }
                transactionToEdit = null
            },
            onDelete = {
                val toDelete = transactionToEdit
                if (toDelete != null) {
                    viewModel.deleteTransaction(toDelete)
                }
                transactionToEdit = null
                showEditTransactionDialog = false
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
            onDismiss = { showNetworkStatusDialog = false }
        )
    }

    if (showBackupDialog) {
        com.example.ui.screens.BackupDialog(
            onExportBackup = { viewModel.exportBackupJson() },
            onImportBackup = { json -> viewModel.importBackupJson(json) },
            onDismiss = { showBackupDialog = false }
        )
    }
}
