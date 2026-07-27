package com.example

import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [TransactionEntity] isOpeningBalance field.
 */
class TransactionEntityTest {

    @Test
    fun `transaction defaults to isOpeningBalance false`() {
        val tx = TransactionEntity(
            id = "test-1",
            customerId = "cust-1",
            type = TransactionType.DEBT,
            amount = 5000.0,
            deviceId = "DEV-TEST"
        )
        assertFalse(tx.isOpeningBalance)
    }

    @Test
    fun `transaction can set isOpeningBalance true`() {
        val tx = TransactionEntity(
            id = "test-1",
            customerId = "cust-1",
            type = TransactionType.DEBT,
            amount = 5000.0,
            deviceId = "DEV-TEST",
            isOpeningBalance = true
        )
        assertTrue(tx.isOpeningBalance)
    }

    @Test
    fun `opening balance transaction has correct default note`() {
        val tx = TransactionEntity(
            id = "test-3",
            customerId = "cust-3",
            type = TransactionType.DEBT,
            amount = 50000.0,
            deviceId = "DEV-TEST",
            isOpeningBalance = true,
            note = "رصيد افتتاحي منقول"
        )

        assertEquals("رصيد افتتاحي منقول", tx.note)
        assertTrue(tx.isOpeningBalance)
        assertEquals(TransactionType.DEBT, tx.type)
    }

    @Test
    fun `opening balance transaction remains DEBT type`() {
        val tx = TransactionEntity(
            id = "test-4",
            customerId = "cust-4",
            type = TransactionType.DEBT,
            amount = 25000.0,
            deviceId = "DEV-TEST",
            isOpeningBalance = true
        )
        assertEquals(TransactionType.DEBT, tx.type)
        assertTrue(tx.isOpeningBalance)
    }

    @Test
    fun `opening balance false does not affect other fields`() {
        val tx = TransactionEntity(
            id = "test-5",
            customerId = "cust-5",
            type = TransactionType.PAYMENT,
            amount = 10000.0,
            note = "تسديد",
            timestamp = 100000L,
            deviceId = "DEV-001",
            isSynced = true,
            isOpeningBalance = false
        )
        assertEquals("test-5", tx.id)
        assertEquals("cust-5", tx.customerId)
        assertEquals(TransactionType.PAYMENT, tx.type)
        assertEquals(10000.0, tx.amount, 0.001)
        assertEquals("تسديد", tx.note)
        assertEquals(100000L, tx.timestamp)
        assertEquals("DEV-001", tx.deviceId)
        assertTrue(tx.isSynced)
        assertFalse(tx.isOpeningBalance)
    }

    @Test
    fun `multiple opening balance transactions can exist`() {
        val tx1 = TransactionEntity(
            id = "ob-1", customerId = "cust-a", type = TransactionType.DEBT,
            amount = 5000.0, deviceId = "DEV", isOpeningBalance = true
        )
        val tx2 = TransactionEntity(
            id = "ob-2", customerId = "cust-b", type = TransactionType.DEBT,
            amount = 10000.0, deviceId = "DEV", isOpeningBalance = true
        )
        assertTrue(tx1.isOpeningBalance)
        assertTrue(tx2.isOpeningBalance)
        assertNotEquals(tx1.id, tx2.id)
        assertNotEquals(tx1.customerId, tx2.customerId)
    }
}
