package com.example.data.model

import com.example.data.entity.CustomerEntity

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val totalDebt: Double = 0.0,
    val totalPayment: Double = 0.0,
    val transactionCount: Int = 0,
    val lastTransactionTime: Long? = null
) {
    // Net balance: positive means customer owes money to shop (دين علي الزبون)
    // negative means credit/prepaid by customer
    val netBalance: Double get() = customer.openingBalance + totalDebt - totalPayment
}
