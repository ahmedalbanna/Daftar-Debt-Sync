package com.example.data.repository

import com.example.data.dao.CustomerDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.data.imports.ImportRow
import com.example.data.model.CustomerWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class LedgerRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    val deviceId: String
) {
    val customersWithBalance: Flow<List<CustomerWithBalance>> = combine(
        customerDao.getAllCustomers(),
        transactionDao.getAllTransactions()
    ) { customers, transactions ->
        val transactionsByCustomer = transactions.groupBy { it.customerId }
        
        customers.map { customer ->
            val custTxList = transactionsByCustomer[customer.id] ?: emptyList()
            var totalDebt = 0.0
            var totalPayment = 0.0
            var lastTxTime: Long? = null

            custTxList.forEach { tx ->
                when (tx.type) {
                    TransactionType.DEBT -> totalDebt += tx.amount
                    TransactionType.PAYMENT -> totalPayment += tx.amount
                }
                if (lastTxTime == null || tx.timestamp > lastTxTime!!) {
                    lastTxTime = tx.timestamp
                }
            }

            CustomerWithBalance(
                customer = customer,
                totalDebt = totalDebt,
                totalPayment = totalPayment,
                transactionCount = custTxList.size,
                lastTransactionTime = lastTxTime
            )
        }
    }

    fun getCustomerTransactions(customerId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByCustomer(customerId)
    }

    fun getCustomerById(customerId: String): Flow<CustomerEntity?> {
        return customerDao.getCustomerById(customerId)
    }

    suspend fun addCustomer(name: String, phone: String, notes: String): CustomerEntity {
        val newCustomer = CustomerEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone.trim(),
            notes = notes.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        customerDao.insertCustomer(newCustomer)
        return newCustomer
    }

    suspend fun addOpeningBalanceTransaction(
        customerId: String,
        amount: Double
    ): TransactionEntity {
        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            type = TransactionType.DEBT,
            amount = amount,
            note = "رصيد افتتاحي منقول",
            timestamp = System.currentTimeMillis(),
            deviceId = deviceId,
            isSynced = false,
            isOpeningBalance = true
        )
        transactionDao.insertTransaction(transaction)

        val customer = customerDao.getCustomerByIdDirect(customerId)
        if (customer != null) {
            customerDao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
        }

        return transaction
    }

    /**
     * Import customers and their opening balances from a list of [ImportRow] items.
     * Returns the number of successfully imported customers.
     */
    suspend fun importCustomersFromFile(rows: List<ImportRow>): Int {
        var imported = 0
        for (row in rows) {
            // Check if customer with same phone already exists
            val existingCustomers = customerDao.getAllCustomersDirect()
            val duplicate = existingCustomers.any { c ->
                c.phone.isNotBlank() && c.phone == row.phone
            }
            if (duplicate) continue

            val customer = CustomerEntity(
                id = UUID.randomUUID().toString(),
                name = row.name.trim(),
                phone = row.phone.trim(),
                notes = "مستورد من ملف خارجي",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            customerDao.insertCustomer(customer)

            addOpeningBalanceTransaction(customer.id, row.balance)
            imported++
        }
        return imported
    }

    suspend fun addTransaction(
        customerId: String,
        type: TransactionType,
        amount: Double,
        note: String
    ): TransactionEntity {
        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            type = type,
            amount = amount,
            note = note.trim(),
            timestamp = System.currentTimeMillis(),
            deviceId = deviceId,
            isSynced = false
        )
        transactionDao.insertTransaction(transaction)
        
        // Touch customer updatedAt
        val customer = customerDao.getCustomerByIdDirect(customerId)
        if (customer != null) {
            customerDao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
        }
        
        return transaction
    }

    suspend fun getUnsyncedTransactions(): List<TransactionEntity> {
        return transactionDao.getUnsyncedTransactions()
    }

    suspend fun getAllCustomersDirect(): List<CustomerEntity> {
        return customerDao.getAllCustomersDirect()
    }

    suspend fun getAllTransactionsDirect(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsDirect()
    }

    suspend fun importSyncData(
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>
    ) {
        if (customers.isNotEmpty()) {
            customerDao.insertCustomers(customers)
        }
        if (transactions.isNotEmpty()) {
            transactionDao.insertTransactions(transactions)
        }
    }

    suspend fun markTransactionsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            transactionDao.markAsSynced(ids)
        }
    }

    suspend fun populateSampleData() {
        val existing = customerDao.getAllCustomersDirect()
        if (existing.isNotEmpty()) return

        val c1 = CustomerEntity(id = "c1", name = "أحمد المحمود", phone = "07701234567", notes = "زبون دائم - المحل الرئيسي")
        val c2 = CustomerEntity(id = "c2", name = "أبو فهد العراقي", phone = "07809876543", notes = "تسديد أسبوعي")
        val c3 = CustomerEntity(id = "c3", name = "محل الأمل للتجهيزات", phone = "07505554433", notes = "جملة وسوبرماركت")

        customerDao.insertCustomers(listOf(c1, c2, c3))

        val now = System.currentTimeMillis()
        val day = 86400000L

        val txs = listOf(
            TransactionEntity(id = "t1", customerId = "c1", type = TransactionType.DEBT, amount = 25000.0, note = "شراء مواد غذائية وزيت", timestamp = now - 3 * day, deviceId = deviceId, isSynced = true),
            TransactionEntity(id = "t2", customerId = "c1", type = TransactionType.DEBT, amount = 15000.0, note = "كارتات شحن ومشروبات", timestamp = now - 2 * day, deviceId = deviceId, isSynced = true),
            TransactionEntity(id = "t3", customerId = "c1", type = TransactionType.PAYMENT, amount = 20000.0, note = "دفعة حساب نقدية", timestamp = now - day, deviceId = deviceId, isSynced = true),
            
            TransactionEntity(id = "t4", customerId = "c2", type = TransactionType.DEBT, amount = 85000.0, note = "قائمة الأسبوع الماضي", timestamp = now - 5 * day, deviceId = deviceId, isSynced = true),
            TransactionEntity(id = "t5", customerId = "c2", type = TransactionType.PAYMENT, amount = 50000.0, note = "دفعة عبر تحويل محلي", timestamp = now - 2 * day, deviceId = deviceId, isSynced = true),
            
            TransactionEntity(id = "t6", customerId = "c3", type = TransactionType.DEBT, amount = 150000.0, note = "بضاعة كرتونات عصير ومياه", timestamp = now - 4 * day, deviceId = deviceId, isSynced = true),
            TransactionEntity(id = "t7", customerId = "c3", type = TransactionType.PAYMENT, amount = 150000.0, note = "تسديد الحساب بالكامل", timestamp = now - day, deviceId = deviceId, isSynced = true)
        )

        transactionDao.insertTransactions(txs)
    }
}
