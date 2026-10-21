package com.lloyd.attendance.api

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

object Models {

    data class LoginRequest(
        val username: String,
        val password: String,
        val device_id: String,
        val app_version: String = "2.0.0",
        val timezone: String = "Asia/Kolkata",
        val browser_name: String = "AndroidApp",
        val browser_version: String = "1.0",
        val os_name: String = "Android",
        val device_type: String = "mobile"
    )

    data class RefreshRequest(val refresh_token: String)

    data class ApiResponse<T>(
        val status: Boolean = false,
        val message: String? = null,
        val data: T? = null
    )

    data class LoginData(
        val access_token: String? = null,
        val refresh_token: String? = null,
        val expires_in: Int = 0,
        @SerializedName("profile_id") val profileId: Int? = null,
        val name: String? = null,
        @SerializedName("role_id") val roleId: Int? = null,
        @SerializedName("school_id") val schoolId: Int? = null,
        @SerializedName(value = "photo_url", alternate = ["photo", "avatar", "profile_photo", "student_photo", "avatar_url", "image", "photo_path"])
        val photoUrl: String? = null,
        @SerializedName("roll_no") val rollNo: String? = null,
        val user: UserProfile? = null
    )

    data class PaginationMeta(
        @SerializedName("total_items") val totalItems: Int = 0,
        @SerializedName("current_page") val currentPage: Int = 0,
        @SerializedName("page_size") val pageSize: Int = 0,
        @SerializedName("total_pages") val totalPages: Int = 0,
        @SerializedName("current_page_items") val currentPageItems: Int = 0
    )

    data class PaginatedApiResponse<T>(
        val status: Boolean = false,
        val message: String? = null,
        val data: T? = null,
        val meta: PaginationMeta? = null
    )

    // ── Attendance ────────────────────────────────────────────────────────────

    class StudentAttendanceItem {
        @JvmField var id: Long = 0
        @SerializedName("school_id") @JvmField var schoolId: Int? = null
        @SerializedName("class_id") @JvmField var classId: Int? = null
        @SerializedName("semester_id") @JvmField var semesterId: Int? = null
        @SerializedName("section_id") @JvmField var sectionId: Int? = null
        @SerializedName("subject_id") @JvmField var subjectId: Int? = null
        @SerializedName("subject_name") @JvmField var subjectName: String? = null
        @SerializedName("student_id") @JvmField var studentId: Int? = null
        @SerializedName("student_name") @JvmField var studentName: String? = null
        @SerializedName("roll_no") @JvmField var rollNo: String? = null
        @SerializedName("attendance_date") @JvmField var attendanceDate: String? = null // YYYY-MM-DD
        @JvmField var status: String? = null // "Present", "Absent"
        @SerializedName("class_lecture") @JvmField var classLecture: String? = null
        @SerializedName("created_at") @JvmField var createdAt: String? = null
        @SerializedName("created_by_name") @JvmField var createdByName: String? = null

        val isPresent: Boolean
            get() = "Present".equals(status, ignoreCase = true)

        val subjectDisplayName: String
            get() = if (!subjectName.isNullOrEmpty()) subjectName!! else "Class Lecture"

        val facultyDisplayName: String
            get() = if (!createdByName.isNullOrEmpty()) createdByName!! else "Faculty"

        val lectureTimeFormatted: String
            get() {
                val cl = classLecture?.trim()
                if (!cl.isNullOrEmpty()) return cl
                val ca = createdAt?.trim()
                if (!ca.isNullOrEmpty() && ca.length >= 16) {
                    return try {
                        ca.substring(11, 16)
                    } catch (_: Exception) { ca }
                }
                return ""
            }

        val displayDate: String
            get() {
                val raw = attendanceDate?.trim() ?: createdAt?.take(10)?.trim()
                if (raw.isNullOrEmpty()) return "Date not recorded"
                val formats = listOf("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yyyy")
                for (fmt in formats) {
                    try {
                        val d = SimpleDateFormat(fmt, Locale.US).parse(raw)
                        if (d != null) {
                            return SimpleDateFormat("dd MMM yyyy", Locale.US).format(d)
                        }
                    } catch (_: Exception) {}
                }
                return raw
            }

        val displayDayOfWeek: String
            get() {
                val raw = attendanceDate?.trim() ?: createdAt?.take(10)?.trim()
                if (raw.isNullOrEmpty()) return ""
                val formats = listOf("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yyyy")
                for (fmt in formats) {
                    try {
                        val d = SimpleDateFormat(fmt, Locale.US).parse(raw)
                        if (d != null) {
                            return SimpleDateFormat("EEEE", Locale.US).format(d)
                        }
                    } catch (_: Exception) {}
                }
                return ""
            }

        val displayTime: String
            get() {
                val cl = classLecture?.trim()
                if (!cl.isNullOrEmpty() && (cl.contains(":") || cl.contains("AM", ignoreCase = true) || cl.contains("PM", ignoreCase = true))) {
                    return cl
                }
                val ca = createdAt?.trim()
                if (!ca.isNullOrEmpty()) {
                    val formats = listOf(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd HH:mm:ss",
                        "HH:mm:ss"
                    )
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US)
                            if (fmt.contains("'Z'")) {
                                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }
                            val d = sdf.parse(ca)
                            if (d != null) {
                                val outFmt = SimpleDateFormat("hh:mm a", Locale.US)
                                outFmt.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                                return outFmt.format(d)
                            }
                        } catch (_: Exception) {}
                    }
                    if (ca.length >= 16) {
                        return try {
                            val timePart = ca.substring(11, 16)
                            val parts = timePart.split(":")
                            val hr = parts[0].toInt()
                            val min = parts[1]
                            val ampm = if (hr >= 12) "PM" else "AM"
                            val hr12 = if (hr == 0) 12 else if (hr > 12) hr - 12 else hr
                            String.format(Locale.US, "%02d:%s %s", hr12, min, ampm)
                        } catch (_: Exception) { ca }
                    }
                }
                return ""
            }

        val lectureSlotDisplay: String
            get() {
                val cl = classLecture?.trim()
                if (cl.isNullOrEmpty()) return "Lecture"
                return if (cl.all { it.isDigit() }) "Lecture #$cl" else cl
            }

        fun getMonthYearLabel(): String {
            val date = attendanceDate
            if (date != null && date.length >= 7) {
                return try {
                    val parts = date.split("-")
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val months = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    "${months[month]} $year"
                } catch (_: Exception) { "Other" }
            }
            return "Other"
        }

        fun getFormattedDate(): String {
            val date = attendanceDate ?: return "—"
            return try {
                val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outFmt = SimpleDateFormat("EEE, MMM dd", Locale.US)
                val d = inFmt.parse(date)
                if (d != null) outFmt.format(d) else date
            } catch (_: Exception) { date }
        }
    }

    // ── User Profile ──────────────────────────────────────────────────────────

    class UserProfile {
        @SerializedName(value = "id", alternate = ["profile_id", "user_id"])
        @JvmField var id: Int = 0
        @JvmField var name: String? = null
        @JvmField var username: String? = null
        @JvmField var email: String? = null
        @JvmField var role: String? = null
        @JvmField var admission_no: String? = null
        @JvmField var course: String? = null
        @JvmField var semester: String? = null
        @JvmField var section: String? = null
        @SerializedName(value = "photo_url", alternate = ["photo", "avatar_url", "profile_image", "avatar", "profile_photo", "student_photo", "photo_path", "image"])
        @JvmField var photo_url: String? = null
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    data class DashboardData(val dashboard: DashboardContent? = null)
    data class DashboardContent(val profile: UserProfile? = null)

    // ── Profile Me ────────────────────────────────────────────────────────────

    data class ProfileMeData(
        val role: String? = null,
        val profile: ProfileDetail? = null,
        val academic: AcademicDetail? = null
    )

    data class ProfileDetail(
        val name: String? = null,
        val username: String? = null,
        val email: String? = null,
        val phone: String? = null,
        val gender: String? = null,
        @SerializedName("blood_group") val bloodGroup: String? = null,
        val dob: String? = null,
        @SerializedName(value = "photo_url", alternate = ["photo", "avatar_url", "image"])
        val photo_url: String? = null
    )

    data class AcademicDetail(
        @SerializedName("admission_no") val admissionNo: String? = null,
        @SerializedName("class_id") val classId: Int? = null,
        @SerializedName("section_id") val sectionId: Int? = null,
        @SerializedName("class_name") val className: String? = null,
        @SerializedName("section_name") val sectionName: String? = null,
        @SerializedName("semester_name") val semesterName: String? = null,
        @SerializedName("roll_no") val rollNo: String? = null,
        @SerializedName("registration_no") val registrationNo: String? = null
    )

    // ── Monthly / Weekly attendance ───────────────────────────────────────────

    data class MonthlyAttendanceData(
        @SerializedName("student_id") val studentId: Int = 0,
        @SerializedName("student_name") val studentName: String? = null,
        @SerializedName(value = "photo_url", alternate = ["photo", "avatar", "profile_photo", "student_photo", "avatar_url", "image", "photo_path"])
        val photoUrl: String? = null,
        val months: List<MonthItem>? = null
    )

    data class MonthItem(
        val year: Int = 0,
        @SerializedName("month_number") val monthNumber: Int = 0,
        @SerializedName("month_label") val monthLabel: String? = null,
        val present: Int = 0,
        val total: Int = 0,
        val percentage: Double = 0.0
    )

    data class WeeklyAttendanceData(
        @SerializedName("student_id") val studentId: Int = 0,
        @SerializedName("student_name") val studentName: String? = null,
        @SerializedName("week_start") val weekStart: String? = null,
        @SerializedName("week_end") val weekEnd: String? = null,
        @SerializedName("earliest_week_start") val earliestWeekStart: String? = null,
        @SerializedName("latest_week_start") val latestWeekStart: String? = null,
        val days: List<DayItem>? = null
    )

    data class DayItem(
        val date: String? = null,
        val day: String? = null,
        val periods: List<PeriodItem>? = null
    )

    data class PeriodItem(
        @SerializedName("routine_id") val routineId: Int? = null,
        @SerializedName("subject_id") val subjectId: Int? = null,
        @SerializedName("subject_name") val subjectName: String? = null,
        @SerializedName("start_time") val startTime: String? = null,
        @SerializedName("end_time") val endTime: String? = null,
        @SerializedName("room_no") val roomNo: String? = null,
        val status: String? = null, // "present", "absent", "unmarked", etc.
        @SerializedName("teacher_name") val teacherName: String? = null,
        val date: String? = null,
        val day: String? = null
    )

    // ── Calculated stats ──────────────────────────────────────────────────────

    data class SubjectStat(
        var subjectName: String = "",
        var teacherName: String = "",
        var present: Int = 0,
        var absent: Int = 0,
        var total: Int = 0,
        var percentage: Double = 0.0,
        var bunkAllowance: Int = 0,
        var neededToReach75: Int = 0,
        var isSafe: Boolean = false
    )

    class CalculatedStats {
        @JvmField var studentName: String = ""
        @JvmField var totalPresent: Int = 0
        @JvmField var totalClasses: Int = 0
        @JvmField var totalAbsent: Int = 0
        @JvmField var overallPercentage: Double = 0.0
        @JvmField var bunkAllowance: Int = 0
        @JvmField var neededToReach75: Int = 0
        @JvmField var isSafe: Boolean = false
        @JvmField var lastUpdatedMillis: Long = 0

        companion object {
            /**
             * Pure function: computes attendance stats from monthly summary items.
             * Boundary: 0/0 → percentage = 100.0 (NoData treated as safe).
             */
            @JvmStatic
            fun fromMonths(name: String?, months: List<MonthItem>?): CalculatedStats {
                val stats = CalculatedStats()
                stats.studentName = name ?: ""
                stats.lastUpdatedMillis = System.currentTimeMillis()

                if (!months.isNullOrEmpty()) {
                    val p = months.sumOf { it.present }
                    val t = months.sumOf { it.total }
                    stats.totalPresent = p
                    stats.totalClasses = t
                    stats.totalAbsent = max(0, t - p)
                    stats.overallPercentage = if (t > 0) {
                        (p.toDouble() / t * 1000.0).roundToInt() / 10.0
                    } else 100.0
                }

                stats.isSafe = stats.overallPercentage >= 75.0
                if (stats.isSafe) {
                    // (p) / (t + x) >= 0.75  =>  x <= (p - 0.75*t) / 0.75
                    val x = (stats.totalPresent - 0.75 * stats.totalClasses) / 0.75
                    stats.bunkAllowance = max(0, floor(x).toInt())
                    stats.neededToReach75 = 0
                } else {
                    // (p + y) / (t + y) >= 0.75  =>  y >= (0.75*t - p) / 0.25
                    val y = (0.75 * stats.totalClasses - stats.totalPresent) / 0.25
                    stats.neededToReach75 = max(1, ceil(y).toInt())
                    stats.bunkAllowance = 0
                }

                return stats
            }
        }
    }

    // ── Internet resource ─────────────────────────────────────────────────────

    class InternetResourceItem {
        @JvmField var id: Int? = null
        @SerializedName("school_id") @JvmField var schoolId: Int? = null
        @SerializedName("class_id") @JvmField var classId: Int? = null
        @SerializedName("section_id") @JvmField var sectionId: Int? = null
        @SerializedName("student_id") @JvmField var studentId: Int? = null
        @SerializedName("user_id") @JvmField var userId: String? = null  // Campus Wi-Fi / Internet Login ID
        @JvmField var password: String? = null                            // Campus Wi-Fi / Internet Password
        @JvmField var phone: String? = null
        @SerializedName("coordinator_name") @JvmField var coordinatorName: String? = null
        @SerializedName("id_proof") @JvmField var idProof: String? = null
        @JvmField var status: Int? = null  // 1 = Active, 0 = Inactive
        @JvmField var remark: String? = null
    }

    // ── Fee Payments & Receipts ───────────────────────────────────────────────

    class FeeTransactionItem {
        @JvmField var id: Long = 0
        @SerializedName(value = "transaction_id", alternate = ["tx_id", "txn_id", "reference_no"])
        @JvmField var transactionId: String? = null
        @SerializedName(value = "receipt_no", alternate = ["receipt_number", "receiptNo"])
        @JvmField var receiptNo: String? = null
        @SerializedName(value = "amount", alternate = ["paid_amount", "total_amount"])
        @JvmField var amount: Double = 0.0
        @SerializedName(value = "date", alternate = ["payment_date", "created_at", "trans_date"])
        @JvmField var date: String? = null
        @SerializedName(value = "mode", alternate = ["payment_mode", "payment_method", "mode_of_payment"])
        @JvmField var mode: String? = null
        @SerializedName(value = "status", alternate = ["payment_status", "trans_status"])
        @JvmField var status: String? = null
        @SerializedName(value = "academic_year", alternate = ["session", "academicYear"])
        @JvmField var academicYear: String? = null
        @SerializedName(value = "semester", alternate = ["semester_name", "sem"])
        @JvmField var semester: String? = null
        @SerializedName(value = "paid_log_id", alternate = ["paidLogId", "log_id"])
        @JvmField var paidLogId: Long? = null
        @SerializedName(value = "remarks", alternate = ["description", "remark", "notes"])
        @JvmField var remarks: String? = null
    }

    class FeeReceiptItem {
        @JvmField var id: Long = 0
        @SerializedName(value = "receipt_no", alternate = ["receipt_number", "receiptNo"])
        @JvmField var receiptNo: String? = null
        @SerializedName(value = "receipt_date", alternate = ["date", "created_at", "issue_date"])
        @JvmField var receiptDate: String? = null
        @SerializedName(value = "amount", alternate = ["paid_amount", "total_amount"])
        @JvmField var amount: Double = 0.0
        @SerializedName(value = "fee_head", alternate = ["fee_type", "head_name", "category"])
        @JvmField var feeHead: String? = null
        @SerializedName(value = "academic_year", alternate = ["session", "academicYear"])
        @JvmField var academicYear: String? = null
        @SerializedName(value = "semester", alternate = ["semester_name", "sem"])
        @JvmField var semester: String? = null
        @SerializedName(value = "paid_log_id", alternate = ["paidLogId", "log_id"])
        @JvmField var paidLogId: Long? = null
        @SerializedName(value = "slip_status", alternate = ["status", "is_slip_generated"])
        @JvmField var slipStatus: String? = null
        @SerializedName(value = "remarks", alternate = ["remark", "notes"])
        @JvmField var remarks: String? = null
    }
}
