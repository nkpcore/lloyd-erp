package com.lloyd.attendance

import com.google.gson.Gson
import com.lloyd.attendance.feature.syllabus.CourseOutcome
import com.lloyd.attendance.feature.syllabus.SyllabusCourse
import com.lloyd.attendance.feature.syllabus.SyllabusPayload
import com.lloyd.attendance.feature.syllabus.SyllabusUnit
import com.lloyd.attendance.feature.syllabus.getPdfPageForCourse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class SyllabusTest {

    private val gson = Gson()

    private fun loadPayloadRaw(): SyllabusPayload {
        val assetFile = File("src/main/assets/aktu_cse_syllabus_2026_27.json")
        assertTrue("Asset file must exist at ${assetFile.absolutePath}", assetFile.exists())
        return FileInputStream(assetFile).use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, SyllabusPayload::class.java)
            }
        }
    }

    private fun loadNormalizedCourses() = loadPayloadRaw().safeCourses()

    // -------------------------------------------------------------------------
    // Core deserialization tests
    // -------------------------------------------------------------------------

    @Test
    fun testSyllabusJsonLoads20Courses() {
        val payload = loadPayloadRaw()
        assertNotNull("Payload must not be null", payload)
        assertEquals("Must have exactly 20 courses", 20, payload.courses?.size ?: 0)
    }

    @Test
    fun testAllCoursesNormalizeWithoutException() {
        // Exercises the safeCourses() / normalized() path that the production
        // code runs after Gson deserialization — the path that was crashing.
        val courses = loadNormalizedCourses()
        assertEquals(20, courses.size)
        // Every list must be non-null after normalization
        for (course in courses) {
            assertNotNull("objectives non-null for ${course.code}", course.objectives)
            assertNotNull("outcomes non-null for ${course.code}", course.outcomes)
            assertNotNull("units non-null for ${course.code}", course.units)
            assertNotNull("books non-null for ${course.code}", course.books)
            assertNotNull("experiments non-null for ${course.code}", course.experiments)
            assertNotNull("semesters non-null for ${course.code}", course.semesters)
        }
    }

    @Test
    fun testAllRequiredCourseFieldsPresent() {
        val courses = loadNormalizedCourses()
        for (course in courses) {
            assertTrue("id must not be blank for ${course.code}", !course.id.isNullOrBlank())
            assertTrue("code must not be blank for ${course.id}", !course.code.isNullOrBlank())
            assertTrue("title must not be blank for ${course.code}", !course.title.isNullOrBlank())
            assertTrue("credits >= 0 for ${course.code}", course.credits >= 0)
        }
    }

    @Test
    fun testEachCourseHas5Units() {
        val courses = loadNormalizedCourses()
        for (course in courses) {
            val unitCount = course.units?.size ?: 0
            assertTrue(
                "Course ${course.code} should have 5 units, got $unitCount",
                unitCount == 5
            )
        }
    }

    @Test
    fun testNoUnitHasDuplicateUnitNumber() {
        val courses = loadNormalizedCourses()
        for (course in courses) {
            val nums = course.units?.map { it.unitNumber } ?: emptyList()
            assertEquals(
                "Course ${course.code} has duplicate unit numbers: $nums",
                nums.size,
                nums.toSet().size
            )
        }
    }

    @Test
    fun testUnitFieldsNonNullAfterNormalization() {
        val courses = loadNormalizedCourses()
        for (course in courses) {
            course.units?.forEach { unit ->
                assertNotNull("Unit title non-null in ${course.code}", unit.title)
                assertNotNull("Unit weightage non-null in ${course.code}", unit.weightage)
                assertNotNull("Unit topics non-null in ${course.code}", unit.topics)
                assertNotNull("Unit applications non-null in ${course.code}", unit.applications)
            }
        }
    }

    @Test
    fun testCourseOutcomeFieldsNonNull() {
        val courses = loadNormalizedCourses()
        for (course in courses) {
            course.outcomes?.forEach { co ->
                assertNotNull("CO code non-null in ${course.code}", co.code)
                assertNotNull("CO text non-null in ${course.code}", co.text)
            }
        }
    }

    @Test
    fun testAllCoursesSelectableWithShortTitle() {
        // Simulates the detail screen header: shortTitle?.ifBlank { title } ?: title ?: ""
        val courses = loadNormalizedCourses()
        for (course in courses) {
            val displayTitle = course.shortTitle?.ifBlank { course.title ?: "" } ?: course.title ?: ""
            // No NPE, result is a string (may be empty for missing data, but shouldn't throw)
            assertNotNull("Display title must be non-null for ${course.code}", displayTitle)
        }
    }

    // -------------------------------------------------------------------------
    // Gson null-bypass regression tests
    // -------------------------------------------------------------------------

    @Test
    fun testGsonNullBypassSafeNormalization() {
        // Gson bypasses Kotlin constructors and sets missing fields to null.
        // Verify that normalized() corrects this for every optional field.
        val jsonMissingOptionals = """
            {
              "id": "TEST001",
              "code": "TST101",
              "title": "Test Course",
              "shortTitle": null,
              "semesters": null,
              "category": null,
              "type": null,
              "credits": 3,
              "ncrfHours": 45,
              "objectives": null,
              "outcomes": null,
              "units": null,
              "books": null
            }
        """.trimIndent()
        val raw = gson.fromJson(jsonMissingOptionals, SyllabusCourse::class.java)
        // Raw from Gson: nulls everywhere defaults were expected
        // normalized() must fix them all
        val c = raw.normalized()
        assertEquals("", c.shortTitle)
        assertEquals(emptyList<Int>(), c.semesters)
        assertEquals("", c.category)
        assertEquals("", c.type)
        assertEquals(emptyList<String>(), c.objectives)
        assertEquals(emptyList<CourseOutcome>(), c.outcomes)
        assertEquals(emptyList<SyllabusUnit>(), c.units)
        assertEquals(emptyList<String>(), c.books)
        assertEquals(emptyList<String>(), c.experiments)
    }

    @Test
    fun testCourseWithEmptyOptionalCollectionsRendersWithoutCrash() {
        // A course with every optional collection explicitly empty — matches the
        // normalized state and must be renderable without null access.
        val course = SyllabusCourse(
            id = "X01", code = "X101", title = "Empty Optional Course",
            shortTitle = "", semesters = emptyList(), category = "Theory",
            type = "Theory", credits = 4, ncrfHours = 60,
            objectives = emptyList(), outcomes = emptyList(), units = emptyList(),
            books = emptyList(), experiments = emptyList()
        )
        val displayTitle = course.shortTitle?.ifBlank { course.title ?: "" } ?: course.title ?: ""
        assertEquals("Empty Optional Course", displayTitle)
        assertEquals(0, course.objectives?.size ?: 0)
        assertEquals(0, course.units?.size ?: 0)
    }

    @Test
    fun testMalformedPayloadDoesNotReturnNullCourseList() {
        // Payload with null courses list — safeCourses() must return empty list.
        val json = """{"academicYear":"2026","stream":"CSE","university":"AKTU","totalCourses":0,"courses":null}"""
        val raw = gson.fromJson(json, SyllabusPayload::class.java)
        val courses = raw.safeCourses()
        assertNotNull(courses)
        assertEquals(0, courses.size)
    }

    // -------------------------------------------------------------------------
    // PDF page mapping tests (unchanged)
    // -------------------------------------------------------------------------

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

    @Test
    fun testGetPdfPageForCourseInValidRange() {
        val courses = loadNormalizedCourses()
        for (course in courses) {
            val page = getPdfPageForCourse(course.code, course.title)
            assertTrue(
                "Page index must be in range 0..32, was $page for ${course.code}",
                page in 0..32
            )
        }
    }
}
