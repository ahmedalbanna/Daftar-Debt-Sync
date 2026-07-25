package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    DEBT,    // دين (مبلغ على الزبون للمحل)
    PAYMENT  // تسديد (مبلغ دفعه الزبون للمحل)
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val type: TransactionType,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val isSynced: Boolean = false
)
