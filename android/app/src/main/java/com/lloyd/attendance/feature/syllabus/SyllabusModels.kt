package com.lloyd.attendance.feature.syllabus

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SyllabusPayload(
    @SerializedName("academicYear") val academicYear: String,
    @SerializedName("stream") val stream: String,
    @SerializedName("university") val university: String,
    @SerializedName("totalCourses") val totalCourses: Int,
    @SerializedName("courses") val courses: List<SyllabusCourse>
)

typealias AktuCourse = SyllabusCourse

@Keep
data class SyllabusCourse(
    @SerializedName("id") val id: String,
    @SerializedName("code") val code: String,
    @SerializedName("title") val title: String,
    @SerializedName("shortTitle") val shortTitle: String,
    @SerializedName("semesters") val semesters: List<Int>,
    @SerializedName("category") val category: String,
    @SerializedName("type") val type: String,
    @SerializedName("credits") val credits: Int,
    @SerializedName("ncrfHours") val ncrfHours: Int,
    @SerializedName("objectives") val objectives: List<String> = emptyList(),
    @SerializedName("outcomes") val outcomes: List<CourseOutcome> = emptyList(),
    @SerializedName("units") val units: List<SyllabusUnit> = emptyList(),
    @SerializedName("books") val books: List<String> = emptyList(),
    @SerializedName("experiments") val experiments: List<String> = emptyList()
)

@Keep
data class CourseOutcome(
    @SerializedName("code") val code: String,
    @SerializedName("text") val text: String,
    @SerializedName("bloomsLevel") val bloomsLevel: String? = null
)

@Keep
data class SyllabusUnit(
    @SerializedName("unitNumber") val unitNumber: Int,
    @SerializedName("title") val title: String,
    @SerializedName("weightage") val weightage: String,
    @SerializedName("topics") val topics: List<String> = emptyList(),
    @SerializedName("applications") val applications: List<String> = emptyList()
)
