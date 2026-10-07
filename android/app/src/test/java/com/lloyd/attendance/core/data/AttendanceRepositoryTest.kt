package com.lloyd.attendance.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AttendanceRepositoryTest {

    @Test
    fun testDefaultSnapshotInitialization() {
        val emptySnapshot = AttendanceRepository.buildEmptySnapshot()
        assertEquals(DataSourceState.OFFLINE, emptySnapshot.sourceState)
        assertEquals(0, emptySnapshot.overall.totalClasses)
        assertEquals(0, emptySnapshot.records.size)
        assertEquals(0, emptySnapshot.reconciliation.discrepancyCount)
    }

    @Test
    fun testReconciliationSnapshotCreation() {
        val snapshot = AttendanceRepository.buildSnapshot(
            studentName = "Student #101",
            studentId = 101,
            monthlyPresent = 76,
            monthlyTotal = 121,
            ledgerRecords = emptyList(),
            sourceState = DataSourceState.FRESH
        )
        assertNotNull(snapshot)
        assertEquals(121, snapshot.overall.totalClasses)
        assertEquals(76, snapshot.overall.totalPresent)
        assertEquals(121, snapshot.reconciliation.aggregateTotal)
        assertEquals(DataSourceState.FRESH, snapshot.sourceState)
    }
}
