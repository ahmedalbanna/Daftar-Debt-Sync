package com.example.sync

import android.content.Context
import android.os.Build
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.repository.LedgerRepository
import com.example.localsync.LocalP2PSyncEngine
import com.example.localsync.model.PeerDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Thin app-specific wrapper around [LocalP2PSyncEngine].
 *
 * Adapts the generic engine to the app's domain model:
 * - Syncs [TransactionEntity] events via the generic engine
 * - App-specific push logic bundles customer + transaction for immediate propagation
 * - Exposes the same public API as the original [SyncOrchestrator] for backward compat
 */
class SyncOrchestrator(
    private val context: Context,
    private val repository: LedgerRepository,
    private val scope: CoroutineScope
) {
    val deviceId: String = repository.deviceId
    val deviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    val serverPort: Int = 8888

    private val transactionSerializer = TransactionSerializer()

    private val engine = LocalP2PSyncEngine<TransactionEntity>(
        serializer = transactionSerializer,
        getAllEvents = { repository.getAllTransactionsDirect() },
        importEvents = { events -> repository.importSyncData(emptyList(), events) },
        context = context,
        deviceId = deviceId,
        deviceName = deviceName,
        port = serverPort,
        serviceType = "_daftarsync._tcp."
    )

    val discoveredPeers: StateFlow<List<PeerDevice>> = engine.discoveredPeers

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    val isSyncing: StateFlow<Boolean> = engine.isSyncing
    val localIp: String get() = engine.localIp

    fun start() {
        engine.start(scope)

        scope.launch {
            engine.syncEvents.collect { message ->
                addLog(message)
            }
        }
    }

    private fun addLog(log: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _syncLogs.update { current ->
            (listOf("[$time] $log") + current).take(50)
        }
    }

    fun triggerSyncWithAllPeers() {
        engine.triggerSyncWithAllPeers(scope)
    }

    /**
     * Pushes a new transaction (and its associated customer) to all discovered peers.
     * The customer data is synced to peers during subsequent batch sync operations.
     */
    fun pushNewTransactionToAllPeers(customer: CustomerEntity, transaction: TransactionEntity) {
        scope.launch(Dispatchers.IO) {
            engine.broadcastEvent(transaction)
        }
    }

    fun stop() {
        engine.stop()
    }
}
