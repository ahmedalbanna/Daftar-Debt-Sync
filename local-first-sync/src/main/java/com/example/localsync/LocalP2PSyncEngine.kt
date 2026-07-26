package com.example.localsync

import android.content.Context
import com.example.localsync.model.PeerDevice
import com.example.localsync.network.SyncServer
import com.example.localsync.nsd.NsdDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main entry point for the local-first P2P sync library.
 *
 * Composes [NsdDiscovery] for peer discovery and [SyncServer] for data exchange.
 * Apps create an instance with a [SyncSerializer], then call [start] and [stop].
 *
 * @param T The syncable event type to synchronize
 * @param serializer Converts events to/from JSON
 * @param getAllEvents Callback to retrieve all local events for batch sync
 * @param importEvents Callback to persist events received from peers
 * @param context Android context (needed for NSD)
 * @param deviceId Unique identifier for this device
 * @param deviceName Human-readable device name
 * @param port TCP port for the sync server (default 8888)
 * @param serviceType NSD service type for discovery
 */
class LocalP2PSyncEngine<T : SyncableEvent>(
    private val serializer: SyncSerializer<T>,
    private val getAllEvents: suspend () -> List<T>,
    private val importEvents: suspend (List<T>) -> Unit,
    private val context: Context,
    val deviceId: String,
    val deviceName: String,
    private val port: Int = 8888,
    private val serviceType: String = "_p2p-sync._tcp."
) {
    private val nsdDiscovery = NsdDiscovery(
        context = context,
        deviceId = deviceId,
        deviceName = deviceName,
        port = port,
        serviceType = serviceType
    )

    private lateinit var syncServer: SyncServer<T>

    val discoveredPeers: StateFlow<List<PeerDevice>> = nsdDiscovery.discoveredPeers

    val syncEvents: SharedFlow<String> get() = syncServer.syncEvents

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val localIp: String get() = syncServer.localIpAddress

    /**
     * Starts the sync engine: begins listening for connections and discovering peers.
     * Should be called once from the app's initialization code.
     */
    fun start(scope: CoroutineScope) {
        syncServer = SyncServer(
            port = port,
            serializer = serializer,
            getAllEvents = getAllEvents,
            importEvents = importEvents,
            scope = scope
        )
        syncServer.start()
        nsdDiscovery.start()

        // Auto-sync when new peers are discovered
        scope.launch {
            discoveredPeers.collect { peers ->
                if (peers.isNotEmpty()) {
                    triggerSyncWithAllPeers(scope)
                }
            }
        }
    }

    /**
     * Triggers a full batch sync with all currently discovered peers.
     */
    fun triggerSyncWithAllPeers(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            val peers = discoveredPeers.value
            if (peers.isEmpty()) {
                _isSyncing.value = false
                return@launch
            }

            for (peer in peers) {
                syncServer.syncWithPeer(peer.ip, peer.port)
            }
            _isSyncing.value = false
        }
    }

    /**
     * Broadcasts a single event to all currently discovered peers.
     */
    suspend fun broadcastEvent(event: T) {
        val peers = discoveredPeers.value
        for (peer in peers) {
            syncServer.pushEventToPeer(peer.ip, peer.port, event)
        }
    }

    /**
     * Stops the engine: shuts down the server and NSD discovery.
     */
    fun stop() {
        nsdDiscovery.stop()
        if (::syncServer.isInitialized) {
            syncServer.stop()
        }
    }
}
