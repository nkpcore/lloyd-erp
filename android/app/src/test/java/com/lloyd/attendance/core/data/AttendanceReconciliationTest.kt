package com.lloyd.attendance.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceReconciliationTest {

    @Test
    fun testReconciliationCalculation_withRealWorldDiscrepancy() {
        // Real observed ERP values: 76 present / 121 total in monthly aggregate,
        // but 76 present / 36 absent (112 records) in detailed log ledger.
        val reconciliation = AttendanceReconciliation.compute(
            aggregatePresent = 76,
            aggregateTotal = 121,
            ledgerPresent = 76,
            ledgerAbsent = 36
        )

        assertEquals(76, reconciliation.aggregatePresent)
        assertEquals(121, reconciliation.aggregateTotal)
        assertEquals(76, reconciliation.ledgerPresent)
        assertEquals(36, reconciliation.ledgerAbsent)
        assertEquals(112, reconciliation.ledgerTotal)
        assertEquals(9, reconciliation.discrepancyCount)
        assertFalse(reconciliation.isReconciled)
        assertTrue(reconciliation.explanation.contains("9"))
    }

    @Test
    fun testReconciliationCalculation_whenPerfectMatch() {
        val reconciliation = AttendanceReconciliation.compute(
            aggregatePresent = 80,
            aggregateTotal = 100,
            ledgerPresent = 80,
            ledgerAbsent = 20
        )

        assertEquals(0, reconciliation.discrepancyCount)
        assertTrue(reconciliation.isReconciled)
        assertEquals(100, reconciliation.ledgerTotal)
    }

    @Test
    fun testReconciliationCalculation_withZeroTotals() {
        val reconciliation = AttendanceReconciliation.compute(
            aggregatePresent = 0,
            aggregateTotal = 0,
            ledgerPresent = 0,
            ledgerAbsent = 0
        )

        assertEquals(0, reconciliation.discrepancyCount)
        assertTrue(reconciliation.isReconciled)
        assertEquals(0, reconciliation.ledgerTotal)
    }
}
