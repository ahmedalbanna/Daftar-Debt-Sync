package com.example.localsync

import org.json.JSONArray
import org.json.JSONObject

/**
 * Decouples JSON serialization from the sync engine.
 * Apps provide serializer implementations for each entity type they want to sync.
 */
interface SyncSerializer<T : SyncableEvent> {
    fun toJson(event: T): JSONObject
    fun fromJson(json: JSONObject): T
    fun toJsonArray(events: List<T>): JSONArray
    fun fromJsonArray(array: JSONArray): List<T>
    fun entityType(): String
}
