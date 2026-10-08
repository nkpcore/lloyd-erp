package com.lloyd.attendance.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Models {

    public static class LoginRequest {
        public String username;
        public String password;
        public String device_id;
        public String app_version = "2.0.0";
        public String timezone = "Asia/Kolkata";
        public String browser_name = "AndroidApp";
        public String browser_version = "1.0";
        public String os_name = "Android";
        public String device_type = "mobile";

        public LoginRequest(String username, String password, String deviceId) {
            this.username = username;
            this.password = password;
            this.device_id = deviceId;
        }
    }

    public static class RefreshRequest {
        public String refresh_token;

        public RefreshRequest(String refreshToken) {
            this.refresh_token = refreshToken;
        }
    }

    public static class ApiResponse<T> {
        public boolean status;
        public String message;
        public T data;
    }

    public static class LoginData {
        public String access_token;
        public String refresh_token;
        public int expires_in;
        @SerializedName("profile_id")
        public Integer profileId;
        public String name;
        @SerializedName("role_id")
        public Integer roleId;
        @SerializedName("school_id")
        public Integer schoolId;
        @SerializedName(value = "photo_url", alternate = {"photo", "avatar", "profile_photo", "student_photo", "avatar_url", "image", "photo_path"})
        public String photoUrl;
        @SerializedName("roll_no")
        public String rollNo;
        public UserProfile user;
    }

    public static class PaginationMeta {
        @SerializedName("total_items")
        public int totalItems;
        @SerializedName("current_page")
        public int currentPage;
        @SerializedName("page_size")
        public int pageSize;
        @SerializedName("total_pages")
        public int totalPages;
        @SerializedName("current_page_items")
        public int currentPageItems;
    }

    public static class PaginatedApiResponse<T> {
        public boolean status;
        public String message;
        public T data;
        public PaginationMeta meta;
    }

    public static class StudentAttendanceItem {
        public long id;
        @SerializedName("school_id")
        public Integer schoolId;
        @SerializedName("class_id")
        public Integer classId;
        @SerializedName("semester_id")
        public Integer semesterId;
        @SerializedName("section_id")
        public Integer sectionId;
        @SerializedName("subject_id")
        public Integer subjectId;
        @SerializedName("subject_name")
        public String subjectName;
        @SerializedName("student_id")
        public Integer studentId;
        @SerializedName("student_name")
        public String studentName;
        @SerializedName("roll_no")
        public String rollNo;
        @SerializedName("attendance_date")
        public String attendanceDate; // YYYY-MM-DD
        public String status; // "Present", "Absent"
        @SerializedName("class_lecture")
        public String classLecture; // Lecture number string
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("created_by_name")
        public String createdByName; // Faculty name

        public boolean isPresent() {
            return "Present".equalsIgnoreCase(status);
        }

        public String getSubjectDisplayName() {
            return subjectName != null && !subjectName.isEmpty() ? subjectName : "Class Lecture";
        }

        public String getFacultyDisplayName() {
            return createdByName != null && !createdByName.isEmpty() ? createdByName : "Faculty";
        }

        public String getMonthYearLabel() {
            if (attendanceDate != null && attendanceDate.length() >= 7) {
                try {
                    String[] parts = attendanceDate.split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                    return months[month] + " " + year;
                } catch (Exception ignored) {}
            }
            return "Other";
        }

        public String getFormattedDate() {
            if (attendanceDate != null) {
                try {
                    java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                    java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("EEE, MMM dd", java.util.Locale.US);
                    java.util.Date d = inFormat.parse(attendanceDate);
                    if (d != null) return outFormat.format(d);
                } catch (Exception ignored) {}
                return attendanceDate;
            }
            return "—";
        }
    }

    public static class UserProfile {
        @SerializedName(value = "id", alternate = {"profile_id", "user_id"})
        public int id;
        public String name;
        public String username;
        public String email;
        public String role;
        public String admission_no;
        public String course;
        public String semester;
        public String section;
        @SerializedName(value = "photo_url", alternate = {"photo", "avatar_url", "profile_image", "avatar", "profile_photo", "student_photo", "photo_path", "image"})
        public String photo_url;
    }

    public static class DashboardData {
        public DashboardContent dashboard;
    }

    public static class DashboardContent {
        public UserProfile profile;
    }

    public static class ProfileMeData {
        public String role;
        public ProfileDetail profile;
        public AcademicDetail academic;
    }

    public static class ProfileDetail {
        public String name;
        public String username;
        public String email;
        public String phone;
        public String gender;
        @SerializedName("blood_group")
        public String bloodGroup;
        public String dob;
        @SerializedName(value = "photo_url", alternate = {"photo", "avatar_url", "image"})
        public String photo_url;
    }

    public static class AcademicDetail {
        @SerializedName("admission_no")
        public String admissionNo;
        @SerializedName("class_id")
        public Integer classId;
        @SerializedName("section_id")
        public Integer sectionId;
        @SerializedName("class_name")
        public String className;
        @SerializedName("section_name")
        public String sectionName;
        @SerializedName("semester_name")
        public String semesterName;
        @SerializedName("roll_no")
        public String rollNo;
        @SerializedName("registration_no")
        public String registrationNo;
    }

    public static class MonthlyAttendanceData {
        @SerializedName("student_id")
        public int studentId;
        @SerializedName("student_name")
        public String studentName;
        @SerializedName(value = "photo_url", alternate = {"photo", "avatar", "profile_photo", "student_photo", "avatar_url", "image", "photo_path"})
        public String photoUrl;
        public List<MonthItem> months;
    }

    public static class MonthItem {
        public int year;
        @SerializedName("month_number")
        public int monthNumber;
        @SerializedName("month_label")
        public String monthLabel;
        public int present;
        public int total;
        public double percentage;
    }

    public static class WeeklyAttendanceData {
        @SerializedName("student_id")
        public int studentId;
        @SerializedName("student_name")
        public String studentName;
        @SerializedName("week_start")
        public String weekStart;
        @SerializedName("week_end")
        public String weekEnd;
        @SerializedName("earliest_week_start")
        public String earliestWeekStart;
        @SerializedName("latest_week_start")
        public String latestWeekStart;
        public List<DayItem> days;
    }

    public static class DayItem {
        public String date;
        public String day;
        public List<PeriodItem> periods;
    }

    public static class PeriodItem {
        @SerializedName("routine_id")
        public Integer routineId;
        @SerializedName("subject_id")
        public Integer subjectId;
        @SerializedName("subject_name")
        public String subjectName;
        @SerializedName("start_time")
        public String startTime;
        @SerializedName("end_time")
        public String endTime;
        @SerializedName("room_no")
        public String roomNo;
        public String status; // "present", "absent", "unmarked", etc.
        @SerializedName("teacher_name")
        public String teacherName;
        public String date;
        public String day;
    }

    public static class SubjectStat {
        public String subjectName = "";
        public String teacherName = "";
        public int present = 0;
        public int absent = 0;
        public int total = 0;
        public double percentage = 0.0;
        public int bunkAllowance = 0;
        public int neededToReach75 = 0;
        public boolean isSafe = false;
    }

    public static class CalculatedStats {
        public String studentName = "";
        public int totalPresent = 0;
        public int totalClasses = 0;
        public int totalAbsent = 0;
        public double overallPercentage = 0.0;
        public int bunkAllowance = 0;
        public int neededToReach75 = 0;
        public boolean isSafe = false;
        public long lastUpdatedMillis = 0;

        public static CalculatedStats fromMonths(String name, List<MonthItem> months) {
            CalculatedStats stats = new CalculatedStats();
            stats.studentName = name != null ? name : "";
            stats.lastUpdatedMillis = System.currentTimeMillis();

            if (months != null && !months.isEmpty()) {
                int p = 0;
                int t = 0;
                for (MonthItem m : months) {
                    p += m.present;
                    t += m.total;
                }
                stats.totalPresent = p;
                stats.totalClasses = t;
                stats.totalAbsent = Math.max(0, t - p);

                if (t > 0) {
                    stats.overallPercentage = Math.round(((double) p / t) * 1000.0) / 10.0;
                } else {
                    stats.overallPercentage = 100.0;
                }
            }

            stats.isSafe = stats.overallPercentage >= 75.0;
            if (stats.isSafe) {
                // Number of classes student can miss while staying >= 75%
                // (p) / (t + x) >= 0.75 => x <= (p - 0.75 * t) / 0.75
                double x = (stats.totalPresent - 0.75 * stats.totalClasses) / 0.75;
                stats.bunkAllowance = Math.max(0, (int) Math.floor(x));
                stats.neededToReach75 = 0;
            } else {
                // Number of consecutive classes student must attend to reach 75%
                // (p + y) / (t + y) >= 0.75 => y >= (0.75 * t - p) / 0.25
                double y = (0.75 * stats.totalClasses - stats.totalPresent) / 0.25;
                stats.neededToReach75 = Math.max(1, (int) Math.ceil(y));
                stats.bunkAllowance = 0;
            }

            return stats;
        }
    }

    public static class InternetResourceItem {
        public Integer id;
        @SerializedName("school_id")
        public Integer schoolId;
        @SerializedName("class_id")
        public Integer classId;
        @SerializedName("section_id")
        public Integer sectionId;
        @SerializedName("student_id")
        public Integer studentId;
        @SerializedName("user_id")
        public String userId; // Campus Wi-Fi / Internet Login ID
        public String password; // Campus Wi-Fi / Internet Password
        public String phone;
        @SerializedName("coordinator_name")
        public String coordinatorName;
        @SerializedName("id_proof")
        public String idProof;
        public Integer status; // 1 = Active, 0 = Inactive
        public String remark;
    }
}
