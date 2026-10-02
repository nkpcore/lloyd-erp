package com.lloyd.attendance.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Extensible Timetable Repository for Lloyd Institute of Engineering & Technology.
 * Currently serves Section A-1 (Room NB-101, Odd Semester 2026-2027) with modular
 * architecture for future sections, notes, and syllabus integration.
 */
public class TimetableRepository {

    public static class TimetablePeriod {
        public final int periodNumber; // 1 to 7, or 0 for break/test
        public final String periodLabel; // "Period I", "Lunch Break", "Weekly Test"
        public final String startTime;  // "09:00"
        public final String endTime;    // "09:50"
        public final String subjectCode; // "AAS102"
        public final String subjectName; // "Applied Chemistry"
        public final String facultyName; // "Dr. Vivek Das"
        public final String facultyInitials; // "DVD"
        public final String roomNumber;  // "NB-101"
        public final boolean isBreak;
        public final boolean isTest;

        public TimetablePeriod(int periodNumber, String periodLabel, String startTime, String endTime,
                               String subjectCode, String subjectName, String facultyName,
                               String facultyInitials, String roomNumber, boolean isBreak, boolean isTest) {
            this.periodNumber = periodNumber;
            this.periodLabel = periodLabel;
            this.startTime = startTime;
            this.endTime = endTime;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.facultyName = facultyName;
            this.facultyInitials = facultyInitials;
            this.roomNumber = roomNumber;
            this.isBreak = isBreak;
            this.isTest = isTest;
        }
    }

    public static class ActiveClassStatus {
        public final TimetablePeriod activePeriod;
        public final TimetablePeriod nextPeriod;
        public final boolean isOngoing;
        public final int remainingMinutes;

        public ActiveClassStatus(TimetablePeriod activePeriod, TimetablePeriod nextPeriod, boolean isOngoing, int remainingMinutes) {
            this.activePeriod = activePeriod;
            this.nextPeriod = nextPeriod;
            this.isOngoing = isOngoing;
            this.remainingMinutes = remainingMinutes;
        }
    }

    public static boolean isStudentInSectionA1(String section) {
        if (section == null || section.trim().isEmpty()) return true; // Default to Section A-1 for current user
        String clean = section.trim().toUpperCase(Locale.US).replace(" ", "").replace("-", "");
        return clean.contains("A1") || clean.contains("SECTIONA");
    }

    /**
     * Get day schedule for Section A-1.
     * @param calendarDay Calendar.MONDAY (2) through Calendar.SATURDAY (7)
     */
    public static List<TimetablePeriod> getScheduleForDay(int calendarDay) {
        List<TimetablePeriod> list = new ArrayList<>();
        String room = "NB-101";

        switch (calendarDay) {
            case Calendar.MONDAY:
                list.add(new TimetablePeriod(1, "Period I", "09:00", "09:50", "AAS102", "Applied Chemistry", "Dr. Vivek Das", "DVD", room, false, false));
                list.add(new TimetablePeriod(2, "Period II", "09:50", "10:40", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(3, "Period III", "10:40", "11:30", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(4, "Period IV", "11:30", "12:20", "AME101", "Fundamentals of Mechanical Engineering", "Mr. Mukesh Kumar", "MK", room, false, false));
                list.add(new TimetablePeriod(0, "Lunch", "12:20", "13:10", "BREAK", "Lunch Break", "", "", "Cafeteria", true, false));
                list.add(new TimetablePeriod(5, "Period V", "13:10", "14:00", "AEC101", "Fundamentals of Electronics Engineering", "Dr. Ridhima", "DR", room, false, false));
                list.add(new TimetablePeriod(6, "Period VI", "14:00", "14:50", "ACS101", "Programming Language", "Pankaj Kumar", "PK", room, false, false));
                list.add(new TimetablePeriod(7, "Period VII", "14:50", "15:40", "ACS101", "Programming Language", "Pankaj Kumar", "PK", room, false, false));
                list.add(new TimetablePeriod(0, "Weekly Test", "15:50", "16:50", "WT", "Improvement Test", "Department Faculty", "", room, false, true));
                break;

            case Calendar.TUESDAY:
                list.add(new TimetablePeriod(1, "Period I", "09:00", "09:50", "ACS101", "Programming Language", "Pankaj Kumar", "PK", room, false, false));
                list.add(new TimetablePeriod(2, "Period II", "09:50", "10:40", "AME101", "Fundamentals of Mechanical Engineering", "Mr. Mukesh Kumar", "MK", room, false, false));
                list.add(new TimetablePeriod(3, "Period III", "10:40", "11:30", "AAS102", "Applied Chemistry", "Dr. Vivek Das", "DVD", room, false, false));
                list.add(new TimetablePeriod(4, "Period IV", "11:30", "12:20", "AEC101", "Fundamentals of Electronics Engineering", "Dr. Ridhima", "DR", room, false, false));
                list.add(new TimetablePeriod(0, "Lunch", "12:20", "13:10", "BREAK", "Lunch Break", "", "", "Cafeteria", true, false));
                list.add(new TimetablePeriod(5, "Period V", "13:10", "14:00", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(6, "Period VI", "14:00", "14:50", "AAC201", "Environment & Sustainability", "Dr. Divya Gairola", "DDG", room, false, false));
                list.add(new TimetablePeriod(7, "Period VII", "14:50", "15:40", "AAS105", "Professional Communication & Technical Writing", "Ms. Mitali Gupta", "MG", room, false, false));
                list.add(new TimetablePeriod(0, "Weekly Test", "15:50", "16:50", "WT", "Improvement Test", "Department Faculty", "", room, false, true));
                break;

            case Calendar.WEDNESDAY:
                list.add(new TimetablePeriod(1, "Period I", "09:00", "09:50", "AAS102", "Applied Chemistry", "Dr. Vivek Das", "DVD", room, false, false));
                list.add(new TimetablePeriod(2, "Period II", "09:50", "10:40", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(3, "Period III", "10:40", "11:30", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(4, "Period IV", "11:30", "12:20", "AAS105", "Professional Communication & Technical Writing", "Ms. Mitali Gupta", "MG", room, false, false));
                list.add(new TimetablePeriod(0, "Lunch", "12:20", "13:10", "BREAK", "Lunch Break", "", "", "Cafeteria", true, false));
                list.add(new TimetablePeriod(5, "Period V", "13:10", "14:00", "AAS102", "Applied Chemistry", "Dr. Vivek Das", "DVD", room, false, false));
                list.add(new TimetablePeriod(6, "Period VI", "14:00", "14:50", "ACS101", "Programming Language", "Pankaj Kumar", "PK", room, false, false));
                list.add(new TimetablePeriod(7, "Period VII", "14:50", "15:40", "AME101", "Fundamentals of Mechanical Engineering", "Mr. Mukesh Kumar", "MK", room, false, false));
                list.add(new TimetablePeriod(0, "Weekly Test", "15:50", "16:50", "WT", "Improvement Test", "Department Faculty", "", room, false, true));
                break;

            case Calendar.THURSDAY:
                list.add(new TimetablePeriod(1, "Period I", "09:00", "09:50", "ACS101", "Programming Language", "Pankaj Kumar", "PK", room, false, false));
                list.add(new TimetablePeriod(2, "Period II", "09:50", "10:40", "AEC101", "Fundamentals of Electronics Engineering", "Dr. Ridhima", "DR", room, false, false));
                list.add(new TimetablePeriod(3, "Period III", "10:40", "11:30", "AAS102", "Applied Chemistry", "Dr. Vivek Das", "DVD", room, false, false));
                list.add(new TimetablePeriod(4, "Period IV", "11:30", "12:20", "AME101", "Fundamentals of Mechanical Engineering", "Mr. Mukesh Kumar", "MK", room, false, false));
                list.add(new TimetablePeriod(0, "Lunch", "12:20", "13:10", "BREAK", "Lunch Break", "", "", "Cafeteria", true, false));
                list.add(new TimetablePeriod(5, "Period V", "13:10", "14:00", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(6, "Period VI", "14:00", "14:50", "AEC101", "Fundamentals of Electronics Engineering", "Dr. Ridhima", "DR", room, false, false));
                list.add(new TimetablePeriod(7, "Period VII", "14:50", "15:40", "AAC201", "Environment & Sustainability", "Dr. Divya Gairola", "DDG", room, false, false));
                list.add(new TimetablePeriod(0, "Weekly Test", "15:50", "16:50", "WT", "Improvement Test", "Department Faculty", "", room, false, true));
                break;

            case Calendar.FRIDAY:
                list.add(new TimetablePeriod(1, "Period I", "09:00", "09:50", "AAS102", "Applied Chemistry", "Dr. Vivek Das", "DVD", room, false, false));
                list.add(new TimetablePeriod(2, "Period II", "09:50", "10:40", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(3, "Period III", "10:40", "11:30", "AAS103", "Applied Mathematics-I", "Dr. Digvijay Singh", "DDS", room, false, false));
                list.add(new TimetablePeriod(4, "Period IV", "11:30", "12:20", "ACS101", "Programming Language", "Pankaj Kumar", "PK", room, false, false));
                list.add(new TimetablePeriod(0, "Lunch", "12:20", "13:10", "BREAK", "Lunch Break", "", "", "Cafeteria", true, false));
                list.add(new TimetablePeriod(5, "Period V", "13:10", "14:00", "AAS102", "Applied Chemistry", "Dr. Vivek Das", "DVD", room, false, false));
                list.add(new TimetablePeriod(6, "Period VI", "14:00", "14:50", "AME101", "Fundamentals of Mechanical Engineering", "Mr. Mukesh Kumar", "MK", room, false, false));
                list.add(new TimetablePeriod(7, "Period VII", "14:50", "15:40", "AEC101", "Fundamentals of Electronics Engineering", "Dr. Ridhima", "DR", room, false, false));
                break;

            case Calendar.SATURDAY:
                list.add(new TimetablePeriod(0, "Weekly Test", "09:00", "13:10", "WT", "Weekly Test (Every Working Saturday)", "Academic Dept", "", "Exam Hall", false, true));
                list.add(new TimetablePeriod(0, "Library", "13:10", "15:40", "LIB", "Self Study & Research", "Central Library", "", "Library", false, false));
                break;
        }

        return list;
    }

    /**
     * Determines current active class and the next upcoming class.
     */
    public static ActiveClassStatus getActiveOrNextClass() {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SUNDAY) {
            return null; // Closed on Sunday
        }

        List<TimetablePeriod> todaySchedule = getScheduleForDay(day);
        if (todaySchedule.isEmpty()) return null;

        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (int i = 0; i < todaySchedule.size(); i++) {
            TimetablePeriod period = todaySchedule.get(i);
            int startMins = parseMinutes(period.startTime);
            int endMins = parseMinutes(period.endTime);

            if (currentMinutes >= startMins && currentMinutes < endMins) {
                // Class is currently ongoing
                TimetablePeriod next = (i + 1 < todaySchedule.size()) ? todaySchedule.get(i + 1) : null;
                return new ActiveClassStatus(period, next, true, endMins - currentMinutes);
            } else if (currentMinutes < startMins) {
                // Next upcoming class today
                return new ActiveClassStatus(null, period, false, startMins - currentMinutes);
            }
        }

        // All classes ended for today
        return null;
    }

    private static int parseMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }
}
