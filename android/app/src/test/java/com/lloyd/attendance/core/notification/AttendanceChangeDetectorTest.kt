package com.lloyd.attendance.core.notification

import com.lloyd.attendance.api.Models
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceChangeDetectorTest {

    private fun createItem(
        id: Long,
        subject: String,
        status: String?,
        faculty: String = "Dr. Sharma",
        lecture: String = "1"
    ): Models.StudentAttendanceItem {
        val item = Models.StudentAttendanceItem()
        item.id = id
        item.subjectName = subject
        item.status = status
        item.createdByName = faculty
        item.classLecture = lecture
        item.attendanceDate = "2026-10-05"
        return item
    }

    @Test
    fun initialBootstrap_suppressesAllHistoricalRecords() {
        val logs = listOf(
            createItem(1001, "Operating Systems", "Present"),
            createItem(1000, "Database Management", "Absent"),
            createItem(999, "Computer Networks", "Present")
        )

        // When bootstrapping (first sync after login), never notify
        val events = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = emptySet(),
            isFirstBootstrap = true
        )

        assertTrue("First sync bootstrap must never fire notifications for past classes", events.isEmpty())
    }

    @Test
    fun subsequentSync_noNewRecords_returnsEmpty() {
        val logs = listOf(
            createItem(1001, "Operating Systems", "Present"),
            createItem(1000, "Database Management", "Absent")
        )

        val seenIds = setOf("1000", "1001")

        val events = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = seenIds,
            isFirstBootstrap = false
        )

        assertTrue("When no records were added, 0 notifications must be generated", events.isEmpty())
    }

    @Test
    fun teacherMarksPresent_triggersOnePresentEvent() {
        val logs = listOf(
            createItem(1002, "Cloud Computing", "Present", "Prof. Verma", "3"),
            createItem(1001, "Operating Systems", "Present")
        )

        val seenIds = setOf("1001")

        val events = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = seenIds,
            isFirstBootstrap = false
        )

        assertEquals(1, events.size)
        val event = events[0]
        assertEquals(1002L, event.attendanceId)
        assertEquals("Cloud Computing", event.subjectName)
        assertEquals("Prof. Verma", event.facultyName)
        assertEquals("Lecture #3", event.lectureDetails)
        assertTrue(event.isPresent)
    }

    @Test
    fun teacherMarksAbsent_triggersOneAbsentEvent() {
        val logs = listOf(
            createItem(1003, "Design & Analysis of Algorithms", "Absent", "Dr. Gupta", "2"),
            createItem(1001, "Operating Systems", "Present")
        )

        val seenIds = setOf("1001")

        val events = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = seenIds,
            isFirstBootstrap = false
        )

        assertEquals(1, events.size)
        val event = events[0]
        assertEquals(1003L, event.attendanceId)
        assertEquals("Design & Analysis of Algorithms", event.subjectName)
        assertEquals("Dr. Gupta", event.facultyName)
        assertEquals("Lecture #2", event.lectureDetails)
        assertFalse(event.isPresent)
    }

    @Test
    fun caseInsensitiveStatusHandling() {
        val logs = listOf(
            createItem(1010, "Subject 1", "present"),
            createItem(1011, "Subject 2", "ABSENT")
        )

        val events = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = emptySet(),
            isFirstBootstrap = false
        )

        assertEquals(2, events.size)
        assertTrue(events[0].isPresent)
        assertFalse(events[1].isPresent)
    }

    @Test
    fun multipleClassesMarked_returnedInChronologicalOrder() {
        // ERP logs arrive sorted descending by date/id
        val logs = listOf(
            createItem(1005, "Compiler Design", "Absent"),
            createItem(1004, "Software Engineering", "Present"),
            createItem(1003, "Web Technologies", "Present"),
            createItem(1001, "Operating Systems", "Present")
        )

        val seenIds = setOf("1001")

        val events = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = seenIds,
            isFirstBootstrap = false
        )

        assertEquals(3, events.size)
        // Verify chronological ascending order
        assertEquals(1003L, events[0].attendanceId)
        assertEquals("Web Technologies", events[0].subjectName)

        assertEquals(1004L, events[1].attendanceId)
        assertEquals("Software Engineering", events[1].subjectName)

        assertEquals(1005L, events[2].attendanceId)
        assertEquals("Compiler Design", events[2].subjectName)
    }

    @Test
    fun unknownOrNonAttendanceStatus_isIgnored() {
        val logs = listOf(
            createItem(1020, "Holiday Lecture", "Holiday"),
            createItem(1021, "Cancelled Class", "Cancelled"),
            createItem(1022, "Exempted", "Exempted"),
            createItem(1023, "Null Status", null),
            createItem(1024, "Empty Status", "")
        )

        val events = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = emptySet(),
            isFirstBootstrap = false
        )

        assertTrue("Only explicit Present or Absent marks should generate notifications", events.isEmpty())
    }
}
