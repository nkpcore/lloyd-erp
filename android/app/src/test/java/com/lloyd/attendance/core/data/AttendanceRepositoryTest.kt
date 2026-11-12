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

    @Test
    fun testParseMonthlySubjectSummaries_fromMonthsAggregate() {
        val json = """
            {
              "status": true,
              "data": {
                "student_id": 101,
                "months": [
                  { "year": 2026, "month_number": 9, "month_label": "Sep 2026", "present": 18, "total": 20 },
                  { "year": 2026, "month_number": 10, "month_label": "Oct 2026", "present": 7, "total": 10 }
                ]
              }
            }
        """.trimIndent()

        val summaries = AttendanceRepository.parseMonthlySubjectSummaries(json)
        assertEquals(2, summaries.size)
        assertEquals("Sep 2026", summaries[0].subjectName)
        assertEquals(18, summaries[0].present)
        assertEquals(2, summaries[0].absent)
        assertEquals(20, summaries[0].total)
        assertEquals("Oct 2026", summaries[1].subjectName)
        assertEquals(7, summaries[1].present)
        assertEquals(3, summaries[1].absent)
        assertEquals(10, summaries[1].total)
    }

    @Test
    fun testParseMonthlySubjectSummaries_fromExplicitSubjectsArray() {
        val json = """
            {
              "status": true,
              "data": {
                "subjects": [
                  { "subject_name": "Algorithms", "present": 12, "total": 15 },
                  { "subject_name": "Operating Systems", "present": 9, "absent": 1, "total": 10 }
                ]
              }
            }
        """.trimIndent()

        val summaries = AttendanceRepository.parseMonthlySubjectSummaries(json)
        assertEquals(2, summaries.size)
        assertEquals("Algorithms", summaries[0].subjectName)
        assertEquals(12, summaries[0].present)
        assertEquals(3, summaries[0].absent)
        assertEquals(15, summaries[0].total)
        assertEquals("Operating Systems", summaries[1].subjectName)
        assertEquals(9, summaries[1].present)
        assertEquals(1, summaries[1].absent)
        assertEquals(10, summaries[1].total)
    }

    @Test
    fun testParseMonthlySubjectSummaries_invalidOrEmptyReturnsEmpty() {
        assertEquals(0, AttendanceRepository.parseMonthlySubjectSummaries(null).size)
        assertEquals(0, AttendanceRepository.parseMonthlySubjectSummaries("").size)
        assertEquals(0, AttendanceRepository.parseMonthlySubjectSummaries("{bad json").size)
    }
}
