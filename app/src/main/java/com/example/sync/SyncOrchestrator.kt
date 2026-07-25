package com.example.sync

import android.content.Context
import android.os.Build
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncOrchestrator(
    private val context: Context,
    private val repository: LedgerRepository,
    private val scope: CoroutineScope
) {
    val deviceId: String = repository.deviceId
    val deviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    val serverPort: Int = 8888

    private val localServer = LocalSyncServer(serverPort, repository, scope)
    private val nsdManager = NsdSyncManager(context, deviceId, deviceName, serverPort)

    val discoveredPeers: StateFlow<List<PeerDevice>> = nsdManager.discoveredPeers

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val localIp: String get() = localServer.localIpAddress

    fun start() {
        localServer.start()
        nsdManager.startDiscoveryAndRegistration()

        scope.launch {
            localServer.syncEvents.collect { message ->
                addLog(message)
            }
        }

        scope.launch {
            discoveredPeers.collect { peers ->
                if (peers.isNotEmpty()) {
                    addLog("تم اكتشاف ${peers.size} جهاز على الشبكة المحلية")
                    triggerSyncWithAllPeers()
                }
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
        scope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            val peers = discoveredPeers.value
            if (peers.isEmpty()) {
                addLog("لا توجد أجهزة متصلة حالياً للبدء بالمزامنة")
                _isSyncing.value = false
                return@launch
            }

            for (peer in peers) {
                addLog("جاري المزامنة مع الجهاز: ${peer.name} (${peer.ip})...")
                val success = localServer.syncWithPeer(peer.ip, peer.port)
                if (success) {
                    addLog("تمت المزامنة بنجاح مع ${peer.name}")
                }
            }
            _isSyncing.value = false
        }
    }

    fun pushNewTransactionToAllPeers(customer: CustomerEntity, transaction: TransactionEntity) {
        scope.launch(Dispatchers.IO) {
            val peers = discoveredPeers.value
            for (peer in peers) {
                localServer.pushTransactionToPeer(peer.ip, peer.port, customer, transaction)
            }
        }
    }

    fun stop() {
        nsdManager.stop()
        localServer.stop()
    }
}
