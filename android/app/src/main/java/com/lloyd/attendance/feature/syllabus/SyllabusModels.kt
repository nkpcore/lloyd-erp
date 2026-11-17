package com.lloyd.attendance.feature.syllabus

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SyllabusPayload(
    @SerializedName("academicYear") val academicYear: String?,
    @SerializedName("stream") val stream: String?,
    @SerializedName("university") val university: String?,
    @SerializedName("totalCourses") val totalCourses: Int = 0,
    @SerializedName("courses") val courses: List<SyllabusCourse>?
) {
    fun safeCourses(): List<SyllabusCourse> =
        courses?.map { it.normalized() } ?: emptyList()
}

typealias AktuCourse = SyllabusCourse

@Keep
data class SyllabusCourse(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("shortTitle") val shortTitle: String?,
    @SerializedName("semesters") val semesters: List<Int>?,
    @SerializedName("category") val category: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("credits") val credits: Int = 0,
    @SerializedName("ncrfHours") val ncrfHours: Int = 0,
    @SerializedName("objectives") val objectives: List<String>?,
    @SerializedName("outcomes") val outcomes: List<CourseOutcome>?,
    @SerializedName("units") val units: List<SyllabusUnit>?,
    @SerializedName("books") val books: List<String>?,
    @SerializedName("experiments") val experiments: List<String>?
) {
    /** Returns a copy where every nullable collection is replaced with an empty list. */
    fun normalized(): SyllabusCourse = copy(
        id = id ?: "",
        code = code ?: "",
        title = title ?: "",
        shortTitle = shortTitle ?: "",
        semesters = semesters ?: emptyList(),
        category = category ?: "",
        type = type ?: "",
        objectives = objectives ?: emptyList(),
        outcomes = outcomes?.map { it.normalized() } ?: emptyList(),
        units = units?.map { it.normalized() } ?: emptyList(),
        books = books ?: emptyList(),
        experiments = experiments ?: emptyList()
    )
}

@Keep
data class CourseOutcome(
    @SerializedName("code") val code: String?,
    @SerializedName("text") val text: String?,
    @SerializedName("bloomsLevel") val bloomsLevel: String?
) {
    fun normalized(): CourseOutcome = copy(
        code = code ?: "",
        text = text ?: ""
    )
}

@Keep
data class SyllabusUnit(
    @SerializedName("unitNumber") val unitNumber: Int = 0,
    @SerializedName("title") val title: String?,
    @SerializedName("weightage") val weightage: String?,
    @SerializedName("topics") val topics: List<String>?,
    @SerializedName("applications") val applications: List<String>?
) {
    fun normalized(): SyllabusUnit = copy(
        title = title ?: "",
        weightage = weightage ?: "",
        topics = topics ?: emptyList(),
        applications = applications ?: emptyList()
    )
}
