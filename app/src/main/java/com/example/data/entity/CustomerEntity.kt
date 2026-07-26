package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val openingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
