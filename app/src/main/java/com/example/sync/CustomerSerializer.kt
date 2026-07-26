package com.example.sync

import com.example.data.entity.CustomerEntity
import com.example.localsync.SyncSerializer
import org.json.JSONArray
import org.json.JSONObject

class CustomerSerializer : SyncSerializer<CustomerEntity> {

    override fun toJson(event: CustomerEntity): JSONObject {
        return JSONObject().apply {
            put("id", event.id)
            put("name", event.name)
            put("phone", event.phone)
            put("notes", event.notes)
            put("createdAt", event.createdAt)
            put("updatedAt", event.updatedAt)
        }
    }

    override fun fromJson(json: JSONObject): CustomerEntity {
        return CustomerEntity(
            id = json.getString("id"),
            name = json.getString("name"),
            phone = json.optString("phone", ""),
            notes = json.optString("notes", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    override fun toJsonArray(events: List<CustomerEntity>): JSONArray {
        val array = JSONArray()
        events.forEach { array.put(toJson(it)) }
        return array
    }

    override fun fromJsonArray(array: JSONArray): List<CustomerEntity> {
        val list = mutableListOf<CustomerEntity>()
        for (i in 0 until array.length()) {
            list.add(fromJson(array.getJSONObject(i)))
        }
        return list
    }

    override fun entityType(): String = "CUSTOMER"
}
