package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.data.model.CustomerWithBalance
import com.example.data.repository.LedgerRepository
import com.example.sync.PeerDevice
import com.example.sync.SyncJsonAdapter
import com.example.sync.SyncOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    
    // Unique device ID per installation
    val deviceId: String = getOrCreateDeviceId()

    val repository = LedgerRepository(
        customerDao = db.customerDao(),
        transactionDao = db.transactionDao(),
        deviceId = deviceId
    )

    val syncOrchestrator = SyncOrchestrator(
        context = application.applicationContext,
        repository = repository,
        scope = viewModelScope
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> = combine(
        repository.customersWithBalance,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter {
                it.customer.name.lowercase().contains(q) ||
                        it.customer.phone.contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedCustomerDetail: StateFlow<CustomerWithBalance?> = combine(
        repository.customersWithBalance,
        _selectedCustomerId
    ) { list, id ->
        if (id == null) null else list.find { it.customer.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val selectedCustomerTransactions: StateFlow<List<TransactionEntity>> = combine(
        _selectedCustomerId,
        db.transactionDao().getAllTransactions()
    ) { id, txs ->
        if (id == null) emptyList()
        else txs.filter { it.customerId == id }.sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalShopDebt: StateFlow<Double> = repository.customersWithBalance.combine(MutableStateFlow(Unit)) { list, _ ->
        list.sumOf { if (it.netBalance > 0) it.netBalance else 0.0 }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val totalShopPayment: StateFlow<Double> = repository.customersWithBalance.combine(MutableStateFlow(Unit)) { list, _ ->
        list.sumOf { it.totalPayment }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val discoveredPeers: StateFlow<List<PeerDevice>> = syncOrchestrator.discoveredPeers
    val syncLogs: StateFlow<List<String>> = syncOrchestrator.syncLogs
    val isSyncing: StateFlow<Boolean> = syncOrchestrator.isSyncing

    init {
        syncOrchestrator.start()
        viewModelScope.launch {
            repository.populateSampleData()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCustomer(customerId: String?) {
        _selectedCustomerId.value = customerId
    }

    fun addCustomer(name: String, phone: String, notes: String, openingBalance: Double = 0.0) {
        viewModelScope.launch {
            val customer = repository.addCustomer(name, phone, notes, openingBalance)
            selectCustomer(customer.id)
        }
    }

    fun addTransaction(customerId: String, type: TransactionType, amount: Double, note: String) {
        viewModelScope.launch {
            val tx = repository.addTransaction(customerId, type, amount, note)
            val cust = db.customerDao().getCustomerByIdDirect(customerId)
            if (cust != null) {
                syncOrchestrator.pushNewTransactionToAllPeers(cust, tx)
            }
        }
    }

    fun triggerManualSync() {
        syncOrchestrator.triggerSyncWithAllPeers()
    }

    suspend fun exportBackupJson(): String {
        return repository.exportDatabaseToJson()
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            repository.importDatabaseFromJson(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun populateSampleData() {
        viewModelScope.launch {
            repository.populateSampleData()
        }
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = getApplication<Application>().getSharedPreferences("daftar_prefs", Application.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "DEV-" + UUID.randomUUID().toString().take(6).uppercase()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    override fun onCleared() {
        super.onCleared()
        syncOrchestrator.stop()
    }
}
