package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.localsync.SyncableEvent
import java.util.UUID

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    override val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    override val deviceId: String = "Unknown",
    override val entityType: String = "CUSTOMER"
) : SyncableEvent {
    override val timestamp: Long get() = createdAt
}
