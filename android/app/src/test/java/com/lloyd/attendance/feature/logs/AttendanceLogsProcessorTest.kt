package com.lloyd.attendance.feature.logs

import com.lloyd.attendance.api.Models
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceLogsProcessorTest {

    private fun createLog(
        id: Long,
        subject: String,
        faculty: String,
        date: String,
        status: String
    ): Models.StudentAttendanceItem {
        val item = Models.StudentAttendanceItem()
        item.id = id
        item.subjectName = subject
        item.createdByName = faculty
        item.attendanceDate = date
        item.status = status
        item.classLecture = "1"
        return item
    }

    private val sampleLogs = listOf(
        createLog(1, "Software Engineering", "Dr. Digvijay Singh", "05-Oct-2026", "Present"),
        createLog(2, "Compiler Design", "Prof. Ananya Gupta", "05-Oct-2026", "Absent"),
        createLog(3, "Software Engineering", "Dr. Digvijay Singh", "04-Oct-2026", "Present"),
        createLog(4, "Data Mining", "Dr. Rajesh Kumar", "04-Oct-2026", "Present"),
        createLog(5, "Compiler Design", "Prof. Ananya Gupta", "03-Oct-2026", "Present")
    )

    @Test
    fun filterLogs_emptyQueryAndAllFilter_returnsAllItems() {
        val result = AttendanceLogsProcessor.filterLogs(sampleLogs, "", LogStatusFilter.ALL)
        assertEquals(5, result.size)
    }

    @Test
    fun filterLogs_byTeacherName_isCaseInsensitive() {
        val result = AttendanceLogsProcessor.filterLogs(sampleLogs, "digvijay", LogStatusFilter.ALL)
        assertEquals(2, result.size)
        assertTrue(result.all { it.createdByName!!.contains("Digvijay", ignoreCase = true) })
    }

    @Test
    fun filterLogs_bySubjectName_matchesCorrectly() {
        val result = AttendanceLogsProcessor.filterLogs(sampleLogs, "Compiler", LogStatusFilter.ALL)
        assertEquals(2, result.size)
        assertTrue(result.all { it.subjectName!!.contains("Compiler", ignoreCase = true) })
    }

    @Test
    fun filterLogs_byDate_matchesCorrectly() {
        val result = AttendanceLogsProcessor.filterLogs(sampleLogs, "05-Oct", LogStatusFilter.ALL)
        assertEquals(2, result.size)
        assertTrue(result.all { it.attendanceDate == "05-Oct-2026" })
    }

    @Test
    fun filterLogs_statusPresentFilter_returnsOnlyPresent() {
        val result = AttendanceLogsProcessor.filterLogs(sampleLogs, "", LogStatusFilter.PRESENT)
        assertEquals(4, result.size)
        assertTrue(result.all { it.isPresent })
    }

    @Test
    fun filterLogs_statusAbsentFilter_returnsOnlyAbsent() {
        val result = AttendanceLogsProcessor.filterLogs(sampleLogs, "", LogStatusFilter.ABSENT)
        assertEquals(1, result.size)
        assertEquals("Compiler Design", result[0].subjectName)
        assertEquals("Absent", result[0].status)
    }

    @Test
    fun filterLogs_combinedSearchAndStatusFilter() {
        // Search "Compiler Design" + filter PRESENT
        val result = AttendanceLogsProcessor.filterLogs(sampleLogs, "compiler", LogStatusFilter.PRESENT)
        assertEquals(1, result.size)
        assertEquals(5L, result[0].id)
    }

    @Test
    fun groupByDate_groupsRecordsByDateHeader() {
        val grouped = AttendanceLogsProcessor.groupByDate(sampleLogs)
        assertEquals(3, grouped.keys.size)
        assertEquals(2, grouped["05-Oct-2026"]?.size)
        assertEquals(2, grouped["04-Oct-2026"]?.size)
        assertEquals(1, grouped["03-Oct-2026"]?.size)
    }

    @Test
    fun computeSummary_calculatesCorrectCountsAndPercentage() {
        val summary = AttendanceLogsProcessor.computeSummary(sampleLogs)
        assertEquals(5, summary.totalMarked)
        assertEquals(4, summary.totalPresent)
        assertEquals(1, summary.totalAbsent)
        assertEquals(80.0, summary.attendancePercentage, 0.001)
    }

    @Test
    fun computeSummary_emptyList_returnsZeroWithoutNaN() {
        val summary = AttendanceLogsProcessor.computeSummary(emptyList())
        assertEquals(0, summary.totalMarked)
        assertEquals(0, summary.totalPresent)
        assertEquals(0, summary.totalAbsent)
        assertEquals(0.0, summary.attendancePercentage, 0.001)
    }

    @Test
    fun testReconcileSubjectAttendance_matchesMonthlyTotalsAndExtractsFaculty() {
        val logItem1 = Models.StudentAttendanceItem().apply {
            subjectName = "Operating Systems"
            createdByName = "Dr. Sharma"
            status = "Present"
            attendanceDate = "2026-10-01"
        }
        val logItem2 = Models.StudentAttendanceItem().apply {
            subjectName = "Operating Systems"
            createdByName = "Dr. Sharma"
            status = "Absent"
            attendanceDate = "2026-10-02"
        }
        val logs = listOf(logItem1, logItem2)

        val reconciled = AttendanceLogsProcessor.reconcileSubjectAttendance(logs)
        assertEquals(1, reconciled.size)
        val os = reconciled.first()
        assertEquals("Operating Systems", os.subjectName)
        assertEquals("Dr. Sharma", os.teacherName)
        assertEquals(1, os.presentCount)
        assertEquals(2, os.totalClasses)
        assertEquals(50.0, os.percentage.numericValue ?: 0.0, 0.01)
    }
}
