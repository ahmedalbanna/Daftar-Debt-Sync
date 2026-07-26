package com.example.sync

import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import org.json.JSONArray
import org.json.JSONObject

object SyncJsonAdapter {

    fun customerToJson(customer: CustomerEntity): JSONObject {
        return JSONObject().apply {
            put("id", customer.id)
            put("name", customer.name)
            put("phone", customer.phone)
            put("notes", customer.notes)
            put("openingBalance", customer.openingBalance)
            put("createdAt", customer.createdAt)
            put("updatedAt", customer.updatedAt)
        }
    }

    fun jsonToCustomer(json: JSONObject): CustomerEntity {
        return CustomerEntity(
            id = json.getString("id"),
            name = json.getString("name"),
            phone = json.optString("phone", ""),
            notes = json.optString("notes", ""),
            openingBalance = json.optDouble("openingBalance", 0.0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    fun transactionToJson(tx: TransactionEntity): JSONObject {
        return JSONObject().apply {
            put("id", tx.id)
            put("customerId", tx.customerId)
            put("type", tx.type.name)
            put("amount", tx.amount)
            put("note", tx.note)
            put("timestamp", tx.timestamp)
            put("deviceId", tx.deviceId)
        }
    }

    fun jsonToTransaction(json: JSONObject): TransactionEntity {
        return TransactionEntity(
            id = json.getString("id"),
            customerId = json.getString("customerId"),
            type = TransactionType.valueOf(json.optString("type", "DEBT")),
            amount = json.getDouble("amount"),
            note = json.optString("note", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            deviceId = json.optString("deviceId", "Unknown"),
            isSynced = true
        )
    }

    fun customersToJsonArray(customers: List<CustomerEntity>): JSONArray {
        val array = JSONArray()
        customers.forEach { array.put(customerToJson(it)) }
        return array
    }

    fun jsonArrayToCustomers(array: JSONArray): List<CustomerEntity> {
        val list = mutableListOf<CustomerEntity>()
        for (i in 0 until array.length()) {
            list.add(jsonToCustomer(array.getJSONObject(i)))
        }
        return list
    }

    fun transactionsToJsonArray(transactions: List<TransactionEntity>): JSONArray {
        val array = JSONArray()
        transactions.forEach { array.put(transactionToJson(it)) }
        return array
    }

    fun jsonArrayToTransactions(array: JSONArray): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        for (i in 0 until array.length()) {
            list.add(jsonToTransaction(array.getJSONObject(i)))
        }
        return list
    }
}
