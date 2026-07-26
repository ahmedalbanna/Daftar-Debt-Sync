package com.example.localsync

/**
 * Interface for domain entities that can be synchronized via the P2P sync engine.
 * Apps implement this on their data classes to make them syncable across devices.
 */
interface SyncableEvent {
    val id: String
    val timestamp: Long
    val deviceId: String
    val entityType: String
}
