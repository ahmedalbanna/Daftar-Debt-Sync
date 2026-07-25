package com.example.sync

import android.util.Log
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.repository.LedgerRepository
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
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Enumeration

class LocalSyncServer(
    val port: Int,
    private val repository: LedgerRepository,
    private val scope: CoroutineScope
) {
    private val TAG = "LocalSyncServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _syncEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val syncEvents: SharedFlow<String> = _syncEvents.asSharedFlow()

    val localIpAddress: String
        get() {
            try {
                val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
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
                Log.d(TAG, "Local sync server listening on port $port, IP: $localIpAddress")
                _syncEvents.emit("سيرفر المزامنة يعمل على IP: $localIpAddress:$port")

                while (isActive && serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClientConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket exception: ${e.message}")
                _syncEvents.emit("توقف السيرفر المحلي: ${e.message}")
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
            val type = requestJson.optString("action", "")

            when (type) {
                "GET_ALL" -> {
                    val customers = repository.getAllCustomersDirect()
                    val transactions = repository.getAllTransactionsDirect()

                    val response = JSONObject().apply {
                        put("status", "SUCCESS")
                        put("customers", SyncJsonAdapter.customersToJsonArray(customers))
                        put("transactions", SyncJsonAdapter.transactionsToJsonArray(transactions))
                    }
                    writer.println(response.toString())
                    _syncEvents.emit("تم تزويد جهاز آخر بكشف الحركات الكامل")
                }

                "PUSH_TRANSACTION" -> {
                    val customerJson = requestJson.optJSONObject("customer")
                    val txJson = requestJson.optJSONObject("transaction")

                    if (customerJson != null) {
                        val cust = SyncJsonAdapter.jsonToCustomer(customerJson)
                        repository.importSyncData(listOf(cust), emptyList())
                    }
                    if (txJson != null) {
                        val tx = SyncJsonAdapter.jsonToTransaction(txJson)
                        repository.importSyncData(emptyList(), listOf(tx))
                        _syncEvents.emit("تم استقبال حركة دين/تسديد جديدة من جهاز آخر")
                    }

                    val response = JSONObject().apply { put("status", "ACK") }
                    writer.println(response.toString())
                }

                "SYNC_BATCH" -> {
                    val custArray = requestJson.optJSONArray("customers")
                    val txArray = requestJson.optJSONArray("transactions")

                    val customers = if (custArray != null) SyncJsonAdapter.jsonArrayToCustomers(custArray) else emptyList()
                    val transactions = if (txArray != null) SyncJsonAdapter.jsonArrayToTransactions(txArray) else emptyList()

                    repository.importSyncData(customers, transactions)

                    val localTxs = repository.getAllTransactionsDirect()
                    val localCusts = repository.getAllCustomersDirect()

                    val response = JSONObject().apply {
                        put("status", "SYNC_ACK")
                        put("customers", SyncJsonAdapter.customersToJsonArray(localCusts))
                        put("transactions", SyncJsonAdapter.transactionsToJsonArray(localTxs))
                    }
                    writer.println(response.toString())
                    _syncEvents.emit("مزامنة ناجحة مع جهاز آخر (${transactions.size} حركة جديدة)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client socket error", e)
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    suspend fun syncWithPeer(ip: String, peerPort: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(ip, peerPort), 5000)
            socket.soTimeout = 10000

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))

            val myCusts = repository.getAllCustomersDirect()
            val myTxs = repository.getAllTransactionsDirect()

            val req = JSONObject().apply {
                put("action", "SYNC_BATCH")
                put("customers", SyncJsonAdapter.customersToJsonArray(myCusts))
                put("transactions", SyncJsonAdapter.transactionsToJsonArray(myTxs))
            }

            writer.println(req.toString())

            val respLine = reader.readLine()
            if (respLine != null) {
                val respJson = JSONObject(respLine)
                if (respJson.optString("status") == "SYNC_ACK") {
                    val remoteCustArray = respJson.optJSONArray("customers")
                    val remoteTxArray = respJson.optJSONArray("transactions")

                    val remoteCusts = if (remoteCustArray != null) SyncJsonAdapter.jsonArrayToCustomers(remoteCustArray) else emptyList()
                    val remoteTxs = if (remoteTxArray != null) SyncJsonAdapter.jsonArrayToTransactions(remoteTxArray) else emptyList()

                    repository.importSyncData(remoteCusts, remoteTxs)

                    val unsynced = repository.getUnsyncedTransactions()
                    repository.markTransactionsSynced(unsynced.map { it.id })

                    _syncEvents.emit("تمت المزامنة بنجاح مع $ip:$peerPort")
                    socket.close()
                    return@withContext true
                }
            }
            socket.close()
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Failed sync with peer $ip:$peerPort", e)
            _syncEvents.emit("فشلت المزامنة مع $ip: ${e.localizedMessage}")
            return@withContext false
        }
    }

    suspend fun pushTransactionToPeer(ip: String, peerPort: Int, customer: CustomerEntity, transaction: TransactionEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(ip, peerPort), 3000)
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))

            val req = JSONObject().apply {
                put("action", "PUSH_TRANSACTION")
                put("customer", SyncJsonAdapter.customerToJson(customer))
                put("transaction", SyncJsonAdapter.transactionToJson(transaction))
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
