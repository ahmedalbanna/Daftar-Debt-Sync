package com.example.localsync.model

data class PeerDevice(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val status: PeerStatus = PeerStatus.DISCOVERED
)

enum class PeerStatus {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    SYNCING,
    SYNCED,
    OFFLINE
}
