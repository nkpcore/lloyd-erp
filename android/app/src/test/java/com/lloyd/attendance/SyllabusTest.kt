package com.lloyd.attendance

import com.google.gson.Gson
import com.lloyd.attendance.feature.syllabus.SyllabusPayload
import com.lloyd.attendance.feature.syllabus.getPdfPageForCourse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class SyllabusTest {

    @Test
    fun testSyllabusJsonIntegrityAndDeserialization() {
        // Path to syllabus json asset
        val assetFile = File("src/main/assets/aktu_cse_syllabus_2026_27.json")
        assertTrue("Asset file must exist at ${assetFile.absolutePath}", assetFile.exists())

        val gson = Gson()
        val payload: SyllabusPayload = FileInputStream(assetFile).use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, SyllabusPayload::class.java)
            }
        }

        assertNotNull("Payload must not be null", payload)
        assertEquals(20, payload.courses.size)

        for (course in payload.courses) {
            assertNotNull("Course id not null", course.id)
            assertNotNull("Course code not null", course.code)
            assertNotNull("Course title not null", course.title)
            assertNotNull("Course shortTitle not null", course.shortTitle)
            assertTrue("Course credits >= 0", course.credits >= 0)
            assertTrue("Course units not null", course.units != null)
            assertTrue("Course objectives not null", course.objectives != null)
            assertTrue("Course outcomes not null", course.outcomes != null)
            assertTrue("Course books not null", course.books != null)

            // Test getPdfPageForCourse
            val page = getPdfPageForCourse(course.code, course.title)
            assertTrue("Page index must be in range 0..32, was $page for ${course.code}", page in 0..32)

            for (unit in course.units) {
                assertNotNull("Unit title not null", unit.title)
                assertNotNull("Unit weightage not null", unit.weightage)
                assertNotNull("Unit topics not null", unit.topics)
                assertNotNull("Unit applications not null", unit.applications)
            }

            for (co in course.outcomes) {
                assertNotNull("CO code not null", co.code)
                assertNotNull("CO text not null", co.text)
            }
        }
    }

    @Test
    fun testGetPdfPageForCourseMapping() {
        assertEquals(1, getPdfPageForCourse("AAS101E", "Quantum Physics"))
        assertEquals(3, getPdfPageForCourse("AAS102D", "Chemistry"))
        assertEquals(5, getPdfPageForCourse("AAS103D", "Calculus and Linear Algebra"))
        assertEquals(7, getPdfPageForCourse("AAS203D", "Numerical Methods"))
        assertEquals(9, getPdfPageForCourse("AEE101A", "Electrical Engineering"))
        assertEquals(11, getPdfPageForCourse("AEC101A", "Electronics Engineering"))
        assertEquals(13, getPdfPageForCourse("ACS101", "Programming"))
        assertEquals(15, getPdfPageForCourse("ACS201", "Data Structure"))
        assertEquals(17, getPdfPageForCourse("AAS104", "Indian Knowledge System"))
        assertEquals(19, getPdfPageForCourse("AAS105", "Professional Communication"))
        assertEquals(23, getPdfPageForCourse("AAI101", "Artificial Intelligence"))
        assertEquals(25, getPdfPageForCourse("AME101A", "Mechanical Engineering"))
        assertEquals(27, getPdfPageForCourse("AID151", "Design Thinking"))
        assertEquals(31, getPdfPageForCourse("AAC101", "Environment"))
        assertEquals(0, getPdfPageForCourse(null, null))
        assertEquals(0, getPdfPageForCourse("UNKNOWN", "Arbitrary Non-Mapped Subject"))
    }
}
