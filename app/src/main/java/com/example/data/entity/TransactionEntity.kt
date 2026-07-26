package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.localsync.SyncableEvent
import java.util.UUID

enum class TransactionType {
    DEBT,    // دين (مبلغ على الزبون للمحل)
    PAYMENT  // تسديد (مبلغ دفعه الزبون للمحل)
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    override val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val type: TransactionType,
    val amount: Double,
    val note: String = "",
    override val timestamp: Long = System.currentTimeMillis(),
    override val deviceId: String,
    val isSynced: Boolean = false,
    override val entityType: String = "TRANSACTION"
) : SyncableEvent
