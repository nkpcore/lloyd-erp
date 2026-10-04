package com.lloyd.attendance.core.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimetableEngineTest {

    @Test
    fun testParseTimeToMinutes() {
        assertEquals(570, TimetableRepository.parseTimeToMinutes("09:30:00"))
        assertEquals(570, TimetableRepository.parseTimeToMinutes("9:30 AM"))
        assertEquals(855, TimetableRepository.parseTimeToMinutes("02:15 PM"))
        assertEquals(855, TimetableRepository.parseTimeToMinutes("14:15"))
        assertEquals(720, TimetableRepository.parseTimeToMinutes("12:00 PM"))
        assertEquals(0, TimetableRepository.parseTimeToMinutes("12:00 AM"))
        assertEquals(30, TimetableRepository.parseTimeToMinutes("12:30 AM"))
    }

    @Test
    fun testFormatDisplayTime() {
        assertEquals("09:30 AM", TimetableRepository.formatDisplayTime("09:30:00"))
        assertEquals("02:15 PM", TimetableRepository.formatDisplayTime("14:15"))
        assertEquals("12:00 PM", TimetableRepository.formatDisplayTime("12:00"))
        assertEquals("12:45 AM", TimetableRepository.formatDisplayTime("00:45"))
    }

    @Test
    fun testDayNameFromCalendar() {
        assertEquals("Monday", TimetableRepository.getDayNameFromCalendar(Calendar.MONDAY))
        assertEquals("Wednesday", TimetableRepository.getDayNameFromCalendar(Calendar.WEDNESDAY))
        assertEquals("Saturday", TimetableRepository.getDayNameFromCalendar(Calendar.SATURDAY))
        assertEquals("Sunday", TimetableRepository.getDayNameFromCalendar(Calendar.SUNDAY))
    }
}
