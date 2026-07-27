package com.example.sync

import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.localsync.SyncSerializer
import org.json.JSONArray
import org.json.JSONObject

class TransactionSerializer : SyncSerializer<TransactionEntity> {

    override fun toJson(event: TransactionEntity): JSONObject {
        return JSONObject().apply {
            put("id", event.id)
            put("customerId", event.customerId)
            put("type", event.type.name)
            put("amount", event.amount)
            put("note", event.note)
            put("timestamp", event.timestamp)
            put("deviceId", event.deviceId)
            put("isOpeningBalance", event.isOpeningBalance)
        }
    }

    override fun fromJson(json: JSONObject): TransactionEntity {
        return TransactionEntity(
            id = json.getString("id"),
            customerId = json.getString("customerId"),
            type = TransactionType.valueOf(json.optString("type", "DEBT")),
            amount = json.getDouble("amount"),
            note = json.optString("note", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            deviceId = json.optString("deviceId", "Unknown"),
            isSynced = true,
            isOpeningBalance = json.optBoolean("isOpeningBalance", false)
        )
    }

    override fun toJsonArray(events: List<TransactionEntity>): JSONArray {
        val array = JSONArray()
        events.forEach { array.put(toJson(it)) }
        return array
    }

    override fun fromJsonArray(array: JSONArray): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        for (i in 0 until array.length()) {
            list.add(fromJson(array.getJSONObject(i)))
        }
        return list
    }

    override fun entityType(): String = "TRANSACTION"
}
