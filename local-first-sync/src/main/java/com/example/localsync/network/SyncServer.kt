package com.example.localsync.network

import android.util.Log
import com.example.localsync.SyncSerializer
import com.example.localsync.SyncableEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Generic TCP sync server that exchanges events of type [T] with peers.
 *
 * @param T The syncable event type
 * @param port The local port to listen on
 * @param serializer Converts events to/from JSON for network transmission
 * @param getAllEvents Callback to retrieve all local events for batch sync
 * @param importEvents Callback to persist events received from peers
 * @param scope The coroutine scope for server lifecycle
 */
class SyncServer<T : SyncableEvent>(
    val port: Int,
    private val serializer: SyncSerializer<T>,
    private val getAllEvents: suspend () -> List<T>,
    private val importEvents: suspend (List<T>) -> Unit,
    private val scope: CoroutineScope
) {
    private val TAG = "SyncServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _syncEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val syncEvents: SharedFlow<String> = _syncEvents.asSharedFlow()

    val localIpAddress: String
        get() {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting IP", e)
            }
            return "127.0.0.1"
        }

    fun start() {
        if (serverJob != null && serverJob?.isActive == true) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Sync server listening on port $port, IP: $localIpAddress")
                _syncEvents.emit("Sync server running on IP: $localIpAddress:$port")

                while (isActive && serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClientConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket exception: ${e.message}")
                _syncEvents.emit("Server stopped: ${e.message}")
            }
        }
    }

    private suspend fun handleClientConnection(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val line = reader.readLine() ?: return
            val requestJson = JSONObject(line)
            val action = requestJson.optString("action", "")

            when (action) {
                "SYNC_BATCH" -> {
                    val eventsArray = requestJson.optJSONArray("events")
                    val incomingEvents = if (eventsArray != null) {
                        serializer.fromJsonArray(eventsArray)
                    } else {
                        emptyList()
                    }

                    importEvents(incomingEvents)

                    val localEvents = getAllEvents()

                    val response = JSONObject().apply {
                        put("status", "SYNC_ACK")
                        put("entityType", serializer.entityType())
                        put("events", serializer.toJsonArray(localEvents))
                    }
                    writer.println(response.toString())
                    _syncEvents.emit("Batch sync complete (${incomingEvents.size} events received)")
                }

                "EVENT" -> {
                    val eventJson = requestJson.optJSONObject("event")
                    if (eventJson != null) {
                        val event = serializer.fromJson(eventJson)
                        importEvents(listOf(event))
                        _syncEvents.emit("Event received from peer")
                    }
                    val response = JSONObject().apply { put("status", "ACK") }
                    writer.println(response.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client socket error", e)
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Performs a full batch sync with a peer.
     * Sends all local events and receives the peer's events in return.
     */
    suspend fun syncWithPeer(ip: String, peerPort: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, peerPort), 5000)
            socket.soTimeout = 10000

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))

            val localEvents = getAllEvents()

            val req = JSONObject().apply {
                put("action", "SYNC_BATCH")
                put("entityType", serializer.entityType())
                put("events", serializer.toJsonArray(localEvents))
            }

            writer.println(req.toString())

            val respLine = reader.readLine()
            if (respLine != null) {
                val respJson = JSONObject(respLine)
                if (respJson.optString("status") == "SYNC_ACK") {
                    val remoteEventsArray = respJson.optJSONArray("events")
                    val remoteEvents = if (remoteEventsArray != null) {
                        serializer.fromJsonArray(remoteEventsArray)
                    } else {
                        emptyList()
                    }

                    importEvents(remoteEvents)

                    _syncEvents.emit("Sync successful with $ip:$peerPort")
                    socket.close()
                    return@withContext true
                }
            }
            socket.close()
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Failed sync with peer $ip:$peerPort", e)
            _syncEvents.emit("Sync failed with $ip:$peerPort: ${e.localizedMessage}")
            return@withContext false
        }
    }

    /**
     * Pushes a single event to a specific peer.
     */
    suspend fun pushEventToPeer(ip: String, peerPort: Int, event: T): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, peerPort), 3000)
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))

            val req = JSONObject().apply {
                put("action", "EVENT")
                put("entityType", serializer.entityType())
                put("event", serializer.toJson(event))
            }

            writer.println(req.toString())
            val respLine = reader.readLine()
            socket.close()
            return@withContext respLine != null
        } catch (e: Exception) {
            Log.e(TAG, "Push to peer failed: $ip:$peerPort", e)
            return@withContext false
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }
}
