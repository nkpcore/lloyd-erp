package com.lloyd.attendance.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lloyd.attendance.R;
import com.lloyd.attendance.api.ErpApiClient;
import com.lloyd.attendance.api.Models;
import com.lloyd.attendance.data.AppPreferences;
import com.lloyd.attendance.schedule.TimetableRepository;
import com.lloyd.attendance.util.AnimationHelper;
import com.lloyd.attendance.widget.AttendanceSyncWorker;
import com.lloyd.attendance.widget.AttendanceWidgetProvider;
import com.lloyd.attendance.widget.NotificationHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefresh;
    private BottomNavigationView bottomNav;

    // Header & Telemetry
    private TextView tvStudentName;
    private TextView tvStudentInfo;
    private View dotConnectivity;
    private ImageButton btnStealth, btnSync, btnLogout;
    private boolean isStealthMode = false;

    // 4 Content Sections
    private LinearLayout sectionDashboard, sectionSchedule, sectionClasses, sectionSimulator;
    private View currentActiveSection;

    // Section 1: Dashboard
    private TextView tvPercentage, tvBadge;
    private TextView tvStatPresent, tvStatAbsent, tvStatTotal;
    private TextView tvBunkAdvice, tvLastUpdated;
    private ImageView ivBunkIcon;
    private View cardLiveGlance;
    private TextView tvGlanceTitle, tvGlanceDetails, tvGlanceLiveText;
    private ImageView ivGlancePulse;
    private ValueAnimator glancePulseAnimator;

    // Section 2: Schedule
    private TextView tvScheduleTitle;
    private Button btnDayMon, btnDayTue, btnDayWed, btnDayThu, btnDayFri, btnDaySat;
    private Button[] dayButtons;
    private LinearLayout containerSchedulePeriods;
    private int selectedScheduleDay = Calendar.MONDAY;

    // Section 3: Attendance Log
    private Button tabFilterAll, tabFilterPresent, tabFilterAbsent;
    private LinearLayout containerMonthChips, containerSubjectChips;
    private TextView tvFilterSummary;
    private LinearLayout containerPeriods;
    private int currentStatusFilter = 0; // 0=All, 1=Present, 2=Absent
    private String currentMonthFilter = "ALL";
    private String currentSubjectFilter = "ALL";
    private final List<Models.StudentAttendanceItem> allAttendanceLogs = new ArrayList<>();

    // Section 4: Simulator & Subject Analytics
    private Button chipTarget75, chipTarget80;
    private TextView tvSimProjectedPct, tvSimStatusNote, tvSimCurrentLabel;
    private SeekBar sbSimDelta;
    private LinearLayout containerSubjectItems;
    private double targetPercentage = 75.0;
    private int basePresent = 0;
    private int baseTotal = 0;
    private float currentOverallPct = 0f;

    private AppPreferences prefs;
    private ErpApiClient apiClient;
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = AppPreferences.getInstance(this);
        apiClient = new ErpApiClient(this);

        if (!prefs.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Schedule background WorkManager sync
        AttendanceSyncWorker.schedulePeriodicSync(this);

        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        initViews();
        setupNavigation();
        setupScheduleDays();
        setupFilters();
        setupSimulator();

        swipeRefresh.setColorSchemeColors(Color.parseColor("#818CF8"), Color.parseColor("#10B981"));
        swipeRefresh.setOnRefreshListener(this::fetchDataFromErp);

        btnSync.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            swipeRefresh.setRefreshing(true);
            fetchDataFromErp();
        });

        btnStealth.setOnClickListener(v -> toggleStealthMode());
        btnLogout.setOnClickListener(v -> confirmLogout());

        if (cardLiveGlance != null) {
            cardLiveGlance.setOnClickListener(v -> {
                AnimationHelper.animateCardPress(v);
                bottomNav.setSelectedItemId(R.id.nav_schedule);
            });
        }

        // Instantly render locally cached data (<30ms)
        renderCachedData();
        updateLiveClassGlance();

        // Refresh with latest data from college server
        swipeRefresh.setRefreshing(true);
        fetchDataFromErp();
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipe_refresh);
        bottomNav = findViewById(R.id.bottom_navigation);

        tvStudentName = findViewById(R.id.tv_main_student_name);
        tvStudentInfo = findViewById(R.id.tv_main_student_info);
        dotConnectivity = findViewById(R.id.dot_connectivity);
        btnStealth = findViewById(R.id.btn_main_stealth);
        btnSync = findViewById(R.id.btn_main_sync);
        btnLogout = findViewById(R.id.btn_main_logout);

        // Sections
        sectionDashboard = findViewById(R.id.section_dashboard);
        sectionSchedule = findViewById(R.id.section_schedule);
        sectionClasses = findViewById(R.id.section_classes);
        sectionSimulator = findViewById(R.id.section_simulator);
        currentActiveSection = sectionDashboard;

        // Dashboard views
        tvPercentage = findViewById(R.id.tv_main_percentage);
        tvBadge = findViewById(R.id.tv_main_badge);
        tvStatPresent = findViewById(R.id.tv_stat_present);
        tvStatAbsent = findViewById(R.id.tv_stat_absent);
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvBunkAdvice = findViewById(R.id.tv_main_bunk_advice);
        ivBunkIcon = findViewById(R.id.iv_main_bunk_icon);
        tvLastUpdated = findViewById(R.id.tv_main_last_updated);
        cardLiveGlance = findViewById(R.id.card_live_glance);
        tvGlanceTitle = findViewById(R.id.tv_glance_title);
        tvGlanceDetails = findViewById(R.id.tv_glance_details);
        tvGlanceLiveText = findViewById(R.id.tv_glance_live_text);
        ivGlancePulse = findViewById(R.id.iv_glance_pulse);

        // Schedule views
        tvScheduleTitle = findViewById(R.id.tv_schedule_section_title);
        btnDayMon = findViewById(R.id.btn_day_mon);
        btnDayTue = findViewById(R.id.btn_day_tue);
        btnDayWed = findViewById(R.id.btn_day_wed);
        btnDayThu = findViewById(R.id.btn_day_thu);
        btnDayFri = findViewById(R.id.btn_day_fri);
        btnDaySat = findViewById(R.id.btn_day_sat);
        dayButtons = new Button[]{btnDayMon, btnDayTue, btnDayWed, btnDayThu, btnDayFri, btnDaySat};
        containerSchedulePeriods = findViewById(R.id.container_schedule_periods);

        // Classes Log views
        tabFilterAll = findViewById(R.id.tab_filter_all);
        tabFilterPresent = findViewById(R.id.tab_filter_present);
        tabFilterAbsent = findViewById(R.id.tab_filter_absent);
        containerMonthChips = findViewById(R.id.container_month_chips);
        containerSubjectChips = findViewById(R.id.container_subject_chips);
        tvFilterSummary = findViewById(R.id.tv_filter_summary);
        containerPeriods = findViewById(R.id.container_periods);

        // Simulator views
        chipTarget75 = findViewById(R.id.chip_target_75);
        chipTarget80 = findViewById(R.id.chip_target_80);
        tvSimProjectedPct = findViewById(R.id.tv_sim_projected_pct);
        tvSimStatusNote = findViewById(R.id.tv_sim_status_note);
        tvSimCurrentLabel = findViewById(R.id.tv_sim_current_label);
        sbSimDelta = findViewById(R.id.sb_sim_delta);
        containerSubjectItems = findViewById(R.id.container_subject_items);
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            View targetSection = null;

            if (itemId == R.id.nav_dashboard) {
                targetSection = sectionDashboard;
            } else if (itemId == R.id.nav_schedule) {
                targetSection = sectionSchedule;
                renderScheduleDay(selectedScheduleDay);
            } else if (itemId == R.id.nav_classes) {
                targetSection = sectionClasses;
                applyFilters();
            } else if (itemId == R.id.nav_simulator) {
                targetSection = sectionSimulator;
                updateSimulator(sbSimDelta != null ? sbSimDelta.getProgress() - 10 : 0);
            }

            if (targetSection != null && targetSection != currentActiveSection) {
                AnimationHelper.animateCrossFade(currentActiveSection, targetSection);
                currentActiveSection = targetSection;
            }
            return true;
        });
    }

    private void setupScheduleDays() {
        int todayDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (todayDay >= Calendar.MONDAY && todayDay <= Calendar.SATURDAY) {
            selectedScheduleDay = todayDay;
        } else {
            selectedScheduleDay = Calendar.MONDAY;
        }

        int[] days = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
        for (int i = 0; i < dayButtons.length; i++) {
            final int dayIndex = days[i];
            final Button btn = dayButtons[i];
            btn.setOnClickListener(v -> {
                AnimationHelper.animateCardPress(v);
                selectedScheduleDay = dayIndex;
                highlightSelectedDayButton(dayIndex);
                renderScheduleDay(dayIndex);
            });
        }
        highlightSelectedDayButton(selectedScheduleDay);
        renderScheduleDay(selectedScheduleDay);
    }

    private void highlightSelectedDayButton(int calendarDay) {
        int[] days = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
        for (int i = 0; i < dayButtons.length; i++) {
            if (days[i] == calendarDay) {
                dayButtons[i].setBackgroundResource(R.drawable.bg_chip_selected);
                dayButtons[i].setTextColor(Color.WHITE);
            } else {
                dayButtons[i].setBackgroundResource(R.drawable.bg_chip_unselected);
                dayButtons[i].setTextColor(Color.parseColor("#94A3B8"));
            }
        }
    }

    private void renderScheduleDay(int calendarDay) {
        if (containerSchedulePeriods == null) return;
        containerSchedulePeriods.removeAllViews();

        List<TimetableRepository.TimetablePeriod> periods = TimetableRepository.getScheduleForDay(calendarDay);
        LayoutInflater inflater = LayoutInflater.from(this);
        Calendar now = Calendar.getInstance();
        boolean isToday = (now.get(Calendar.DAY_OF_WEEK) == calendarDay);
        int nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (TimetableRepository.TimetablePeriod period : periods) {
            View view = inflater.inflate(R.layout.item_schedule_period, containerSchedulePeriods, false);

            TextView tvTime = view.findViewById(R.id.tv_period_time);
            TextView tvPill = view.findViewById(R.id.tv_period_number_pill);
            TextView tvSubject = view.findViewById(R.id.tv_period_subject_name);
            TextView tvFaculty = view.findViewById(R.id.tv_period_faculty);
            TextView tvRoom = view.findViewById(R.id.tv_period_room);
            View viewAccent = view.findViewById(R.id.view_period_accent);
            View badgeLive = view.findViewById(R.id.badge_live_ongoing);
            ImageView ivPulse = view.findViewById(R.id.iv_live_pulse);
            TextView tvRemaining = view.findViewById(R.id.tv_live_remaining);

            tvTime.setText(period.startTime + " - " + period.endTime);
            tvPill.setText(period.periodLabel);
            tvSubject.setText(period.subjectName);
            tvFaculty.setText(period.facultyName.isEmpty() ? "—" : period.facultyName + (period.facultyInitials.isEmpty() ? "" : " (" + period.facultyInitials + ")"));
            tvRoom.setText(period.roomNumber);

            int startMins = parseMinutes(period.startTime);
            int endMins = parseMinutes(period.endTime);

            if (isToday && nowMins >= startMins && nowMins < endMins) {
                // Ongoing class
                viewAccent.setBackgroundColor(Color.parseColor("#10B981")); // Emerald
                badgeLive.setVisibility(View.VISIBLE);
                tvRemaining.setText("ONGOING • " + (endMins - nowMins) + "m left");
                AnimationHelper.startBreathingAnimation(ivPulse);
            } else if (period.isBreak) {
                viewAccent.setBackgroundColor(Color.parseColor("#F59E0B")); // Amber
            } else if (period.isTest) {
                viewAccent.setBackgroundColor(Color.parseColor("#A78BFA")); // Purple
            } else {
                viewAccent.setBackgroundColor(Color.parseColor("#818CF8")); // Indigo
            }

            containerSchedulePeriods.addView(view);
        }
    }

    private void updateLiveClassGlance() {
        if (cardLiveGlance == null) return;
        TimetableRepository.ActiveClassStatus status = TimetableRepository.getActiveOrNextClass();

        if (status == null) {
            tvGlanceTitle.setText("Classes Done For Today");
            tvGlanceDetails.setText("Room NB-101 • Tomorrow starts at 09:00 AM");
            if (findViewById(R.id.badge_glance_live) != null) {
                findViewById(R.id.badge_glance_live).setVisibility(View.GONE);
            }
            return;
        }

        if (status.isOngoing && status.activePeriod != null) {
            tvGlanceTitle.setText(status.activePeriod.subjectName);
            tvGlanceDetails.setText(status.activePeriod.periodLabel + " (" + status.activePeriod.startTime + " - " + status.activePeriod.endTime + ") • " +
                    status.activePeriod.facultyName);
            tvGlanceLiveText.setText("ONGOING • " + status.remainingMinutes + "m left");
            if (findViewById(R.id.badge_glance_live) != null) {
                findViewById(R.id.badge_glance_live).setVisibility(View.VISIBLE);
            }
            if (glancePulseAnimator != null) glancePulseAnimator.cancel();
            glancePulseAnimator = AnimationHelper.startBreathingAnimation(ivGlancePulse);
        } else if (status.nextPeriod != null) {
            tvGlanceTitle.setText("Next: " + status.nextPeriod.subjectName);
            tvGlanceDetails.setText("Starts at " + status.nextPeriod.startTime + " • " + status.nextPeriod.facultyName + " • " + status.nextPeriod.roomNumber);
            tvGlanceLiveText.setText("IN " + status.remainingMinutes + "m");
            if (findViewById(R.id.badge_glance_live) != null) {
                findViewById(R.id.badge_glance_live).setVisibility(View.VISIBLE);
            }
        }
    }

    private void toggleStealthMode() {
        isStealthMode = !isStealthMode;
        AnimationHelper.animateCardPress(btnStealth);

        if (isStealthMode) {
            btnStealth.setImageResource(R.drawable.ic_visibility_off);
            tvStudentName.setText("••••••••••••••••");
            tvPercentage.setText("••.•%");
            tvStatPresent.setText("••");
            tvStatAbsent.setText("••");
            tvStatTotal.setText("••");
            tvBunkAdvice.setText("Privacy Mode Active");
        } else {
            btnStealth.setImageResource(R.drawable.ic_visibility);
            Models.CalculatedStats cached = prefs.getCachedStats();
            if (cached != null) {
                renderStats(cached);
            }
        }
    }

    private void setupFilters() {
        tabFilterAll.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            currentStatusFilter = 0;
            updateFilterTabsUI();
            applyFilters();
        });

        tabFilterPresent.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            currentStatusFilter = 1;
            updateFilterTabsUI();
            applyFilters();
        });

        tabFilterAbsent.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            currentStatusFilter = 2;
            updateFilterTabsUI();
            applyFilters();
        });
    }

    private void updateFilterTabsUI() {
        tabFilterAll.setBackgroundResource(currentStatusFilter == 0 ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tabFilterAll.setTextColor(currentStatusFilter == 0 ? Color.WHITE : Color.parseColor("#94A3B8"));

        tabFilterPresent.setBackgroundResource(currentStatusFilter == 1 ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tabFilterPresent.setTextColor(currentStatusFilter == 1 ? Color.WHITE : Color.parseColor("#94A3B8"));

        tabFilterAbsent.setBackgroundResource(currentStatusFilter == 2 ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tabFilterAbsent.setTextColor(currentStatusFilter == 2 ? Color.WHITE : Color.parseColor("#94A3B8"));
    }

    private void setupSimulator() {
        chipTarget75.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            targetPercentage = 75.0;
            chipTarget75.setBackgroundResource(R.drawable.bg_chip_selected);
            chipTarget75.setTextColor(Color.WHITE);
            chipTarget80.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipTarget80.setTextColor(Color.parseColor("#94A3B8"));
            updateSimulator(sbSimDelta.getProgress() - 10);
        });

        chipTarget80.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            targetPercentage = 80.0;
            chipTarget80.setBackgroundResource(R.drawable.bg_chip_selected);
            chipTarget80.setTextColor(Color.WHITE);
            chipTarget75.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipTarget75.setTextColor(Color.parseColor("#94A3B8"));
            updateSimulator(sbSimDelta.getProgress() - 10);
        });

        sbSimDelta.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int delta = progress - 10;
                updateSimulator(delta);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateSimulator(int delta) {
        if (baseTotal <= 0) return;

        int simPresent = basePresent;
        int simTotal = baseTotal;

        if (delta >= 0) {
            simPresent += delta;
            simTotal += delta;
            tvSimCurrentLabel.setText("+" + delta + " Attended");
            tvSimCurrentLabel.setTextColor(Color.parseColor("#10B981"));
        } else {
            simTotal += Math.abs(delta);
            tvSimCurrentLabel.setText(delta + " Missed");
            tvSimCurrentLabel.setTextColor(Color.parseColor("#EF4444"));
        }

        double pct = (double) simPresent / simTotal * 100.0;
        tvSimProjectedPct.setText(String.format(Locale.US, "%.1f%%", pct));

        if (pct >= targetPercentage) {
            tvSimProjectedPct.setTextColor(Color.parseColor("#10B981"));
            tvSimStatusNote.setText("Safe! Projected attendance is above the " + (int) targetPercentage + "% requirement.");
        } else {
            tvSimProjectedPct.setTextColor(Color.parseColor("#EF4444"));
            int needed = (int) Math.ceil((targetPercentage * simTotal - 100.0 * simPresent) / (100.0 - targetPercentage));
            if (needed < 0) needed = 0;
            tvSimStatusNote.setText("Shortage warning. Need " + needed + " consecutive classes to reach " + (int) targetPercentage + "%.");
        }
    }

    private void renderCachedData() {
        Models.CalculatedStats cached = prefs.getCachedStats();
        if (cached != null) {
            renderStats(cached);
        }

        // Render cached subject breakdown
        String monthlyJson = prefs.getMonthlyAttendanceJson();
        if (monthlyJson != null && !monthlyJson.isEmpty()) {
            try {
                Models.MonthlyAttendanceData res = gson.fromJson(monthlyJson, Models.MonthlyAttendanceData.class);
                if (res != null) {
                    renderMonthlyData(res);
                }
            } catch (Exception ignored) {}
        }

        // Render cached attendance logs
        String logsJson = prefs.getAttendanceLogsJson();
        if (logsJson != null && !logsJson.isEmpty()) {
            try {
                java.lang.reflect.Type type = new TypeToken<List<Models.StudentAttendanceItem>>() {}.getType();
                List<Models.StudentAttendanceItem> cachedLogs = gson.fromJson(logsJson, type);
                if (cachedLogs != null && !cachedLogs.isEmpty()) {
                    allAttendanceLogs.clear();
                    allAttendanceLogs.addAll(cachedLogs);
                    buildFilterChips();
                    applyFilters();
                }
            } catch (Exception ignored) {}
        }
    }

    private void fetchDataFromErp() {
        executor.execute(() -> {
            try {
                // Fetch stats, subjects, and all attendance logs in parallel
                Future<Models.CalculatedStats> statsFuture = parallelExecutor.submit(() -> apiClient.fetchAndCalculateStats());
                Future<Models.MonthlyAttendanceData> monthlyFuture = parallelExecutor.submit(() -> apiClient.getMonthlyAttendance());
                Future<List<Models.StudentAttendanceItem>> logsFuture = parallelExecutor.submit(() -> apiClient.getStudentAttendanceLogs(prefs.getStudentId()));

                Models.CalculatedStats stats = statsFuture.get();
                Models.MonthlyAttendanceData monthly = monthlyFuture.get();
                List<Models.StudentAttendanceItem> logs = logsFuture.get();

                // Save logs to persistent cache
                if (logs != null && !logs.isEmpty()) {
                    prefs.saveAttendanceLogsJson(gson.toJson(logs));
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    swipeRefresh.setRefreshing(false);

                    // Update connectivity telemetry
                    dotConnectivity.setBackgroundResource(R.drawable.ic_pulse_dot);
                    tvStudentInfo.setText("Live Sync • Section A-1");

                    if (stats != null) {
                        renderStats(stats);
                    }
                    if (monthly != null) {
                        renderMonthlyData(monthly);
                    }
                    if (logs != null && !logs.isEmpty()) {
                        allAttendanceLogs.clear();
                        allAttendanceLogs.addAll(logs);
                        buildFilterChips();
                        applyFilters();
                    }

                    updateLiveClassGlance();
                    AttendanceWidgetProvider.triggerRefresh(MainActivity.this);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    swipeRefresh.setRefreshing(false);

                    // Weak network offline fallback
                    dotConnectivity.setBackgroundColor(Color.parseColor("#64748B"));
                    tvStudentInfo.setText("Cached Mode • Section A-1");

                    renderCachedData();
                    updateLiveClassGlance();
                    Toast.makeText(MainActivity.this, "Offline: Loaded real attendance records from device cache", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderStats(Models.CalculatedStats stats) {
        if (isStealthMode) return;

        if (stats.studentName != null && !stats.studentName.isEmpty()) {
            tvStudentName.setText(stats.studentName);
        }

        basePresent = stats.totalPresent;
        baseTotal = stats.totalClasses;
        currentOverallPct = (float) stats.overallPercentage;

        // Animated Rolling percentage
        AnimationHelper.animateRollingPercentage(tvPercentage, 0f, (float) stats.overallPercentage, 500);

        tvStatPresent.setText(String.valueOf(stats.totalPresent));
        tvStatAbsent.setText(String.valueOf(stats.totalAbsent));
        tvStatTotal.setText(String.valueOf(stats.totalClasses));

        if (stats.lastUpdatedMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            tvLastUpdated.setText(sdf.format(new Date(stats.lastUpdatedMillis)));
        }

        if (stats.isSafe) {
            tvPercentage.setTextColor(Color.parseColor("#10B981"));
            tvBadge.setText(String.format(Locale.US, "SAFE (%.1f%%)", stats.overallPercentage));
            tvBadge.setBackgroundResource(R.drawable.bg_tag_present);
            tvBadge.setTextColor(Color.parseColor("#34D399"));
            ivBunkIcon.setImageResource(R.drawable.ic_check);
            ivBunkIcon.setColorFilter(Color.parseColor("#10B981"));

            if (stats.bunkAllowance > 0) {
                tvBunkAdvice.setText("Can safely bunk " + stats.bunkAllowance + " next class" + (stats.bunkAllowance > 1 ? "es" : ""));
            } else {
                tvBunkAdvice.setText("Safe for now! Don't miss next class.");
            }
        } else {
            tvPercentage.setTextColor(Color.parseColor("#EF4444"));
            tvBadge.setText(String.format(Locale.US, "SHORTAGE (%.1f%%)", stats.overallPercentage));
            tvBadge.setBackgroundResource(R.drawable.bg_tag_absent);
            tvBadge.setTextColor(Color.parseColor("#F87171"));
            ivBunkIcon.setImageResource(R.drawable.ic_alert);
            ivBunkIcon.setColorFilter(Color.parseColor("#EF4444"));

            tvBunkAdvice.setText("Attend next " + stats.neededToReach75 + " class" + (stats.neededToReach75 > 1 ? "es" : "") + " to reach 75%");
        }

        updateSimulator(0);
    }

    private void renderMonthlyData(Models.MonthlyAttendanceData data) {
        if (data == null || data.months == null) return;
        // Also build subject cards in containerSubjectItems
        renderSubjectBreakdown();
    }

    private void renderSubjectBreakdown() {
        if (containerSubjectItems == null || allAttendanceLogs.isEmpty()) return;
        containerSubjectItems.removeAllViews();

        Map<String, int[]> subjectCounts = new LinkedHashMap<>(); // [present, total]
        for (Models.StudentAttendanceItem item : allAttendanceLogs) {
            String name = item.getSubjectDisplayName();
            if (!subjectCounts.containsKey(name)) {
                subjectCounts.put(name, new int[]{0, 0});
            }
            int[] c = subjectCounts.get(name);
            c[1]++;
            if (item.isPresent()) c[0]++;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, int[]> entry : subjectCounts.entrySet()) {
            String subName = entry.getKey();
            int present = entry.getValue()[0];
            int total = entry.getValue()[1];
            double pct = total > 0 ? (double) present / total * 100.0 : 0.0;

            View card = inflater.inflate(R.layout.item_subject, containerSubjectItems, false);
            TextView tvName = card.findViewById(R.id.tv_subject_name);
            TextView tvPct = card.findViewById(R.id.tv_subject_percentage);
            TextView tvCounts = card.findViewById(R.id.tv_subject_stats);
            TextView tvAdvice = card.findViewById(R.id.tv_subject_bunk);
            ProgressBar pb = card.findViewById(R.id.pb_subject_progress);

            tvName.setText(subName);
            tvPct.setText(String.format(Locale.US, "%.1f%%", pct));
            tvCounts.setText(present + " / " + total + " attended");
            pb.setProgress((int) pct);

            if (pct >= 75.0) {
                tvPct.setTextColor(Color.parseColor("#10B981"));
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
                int b = (int) Math.floor((present - 0.75 * total) / 0.75);
                tvAdvice.setText(b > 0 ? "Can safely miss " + b + " class" + (b > 1 ? "es" : "") : "Safe! Don't miss next lecture");
                tvAdvice.setTextColor(Color.parseColor("#34D399"));
            } else {
                tvPct.setTextColor(Color.parseColor("#EF4444"));
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
                int n = (int) Math.ceil((0.75 * total - present) / 0.25);
                tvAdvice.setText("Attend next " + n + " class" + (n > 1 ? "es" : "") + " to hit 75%");
                tvAdvice.setTextColor(Color.parseColor("#F87171"));
            }

            containerSubjectItems.addView(card);
        }
    }

    private void buildFilterChips() {
        if (containerMonthChips == null || containerSubjectChips == null) return;
        containerMonthChips.removeAllViews();
        containerSubjectChips.removeAllViews();

        Map<String, Integer> monthCounts = new LinkedHashMap<>();
        Map<String, Integer> subjectCounts = new LinkedHashMap<>();

        for (Models.StudentAttendanceItem item : allAttendanceLogs) {
            String m = item.getMonthYearLabel();
            monthCounts.put(m, monthCounts.getOrDefault(m, 0) + 1);

            String s = item.getSubjectDisplayName();
            subjectCounts.put(s, subjectCounts.getOrDefault(s, 0) + 1);
        }

        // Add Month Chips
        addChip(containerMonthChips, "All Dates (" + allAttendanceLogs.size() + ")", "ALL", true, val -> {
            currentMonthFilter = val;
            applyFilters();
        });
        for (Map.Entry<String, Integer> e : monthCounts.entrySet()) {
            addChip(containerMonthChips, e.getKey() + " (" + e.getValue() + ")", e.getKey(), false, val -> {
                currentMonthFilter = val;
                applyFilters();
            });
        }

        // Add Subject Chips
        addChip(containerSubjectChips, "All Subjects (" + allAttendanceLogs.size() + ")", "ALL", true, val -> {
            currentSubjectFilter = val;
            applyFilters();
        });
        for (Map.Entry<String, Integer> e : subjectCounts.entrySet()) {
            String shortName = e.getKey().length() > 14 ? e.getKey().substring(0, 12) + ".." : e.getKey();
            addChip(containerSubjectChips, shortName + " (" + e.getValue() + ")", e.getKey(), false, val -> {
                currentSubjectFilter = val;
                applyFilters();
            });
        }
    }

    private void addChip(LinearLayout container, String label, String value, boolean isSelected, ChipClickListener listener) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (34 * getResources().getDisplayMetrics().density)
        );
        lp.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
        btn.setLayoutParams(lp);
        btn.setText(label);
        btn.setTextSize(11);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setAllCaps(false);
        int padH = (int) (12 * getResources().getDisplayMetrics().density);
        btn.setPadding(padH, 0, padH, 0);

        btn.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        btn.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#94A3B8"));

        btn.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof Button) {
                    ((Button) child).setBackgroundResource(R.drawable.bg_chip_unselected);
                    ((Button) child).setTextColor(Color.parseColor("#94A3B8"));
                }
            }
            btn.setBackgroundResource(R.drawable.bg_chip_selected);
            btn.setTextColor(Color.WHITE);
            listener.onClick(value);
        });

        container.addView(btn);
    }

    private interface ChipClickListener {
        void onClick(String value);
    }

    private void applyFilters() {
        if (containerPeriods == null) return;
        containerPeriods.removeAllViews();

        List<Models.StudentAttendanceItem> filtered = new ArrayList<>();
        int countPresent = 0;
        int countAbsent = 0;

        for (Models.StudentAttendanceItem item : allAttendanceLogs) {
            if (item.isPresent()) countPresent++;
            else countAbsent++;

            if (currentStatusFilter == 1 && !item.isPresent()) continue;
            if (currentStatusFilter == 2 && item.isPresent()) continue;
            if (!"ALL".equals(currentMonthFilter) && !currentMonthFilter.equals(item.getMonthYearLabel())) continue;
            if (!"ALL".equals(currentSubjectFilter) && !currentSubjectFilter.equals(item.getSubjectDisplayName())) continue;

            filtered.add(item);
        }

        tabFilterAll.setText("All (" + allAttendanceLogs.size() + ")");
        tabFilterPresent.setText("Present (" + countPresent + ")");
        tabFilterAbsent.setText("Absent (" + countAbsent + ")");

        tvFilterSummary.setText("Showing " + filtered.size() + " of " + allAttendanceLogs.size() + " classes" +
                (!currentMonthFilter.equals("ALL") || !currentSubjectFilter.equals("ALL") || currentStatusFilter != 0 ? " • Filters active" : ""));

        // Group filtered items by date
        Map<String, List<Models.StudentAttendanceItem>> grouped = new LinkedHashMap<>();
        for (Models.StudentAttendanceItem item : filtered) {
            String dateKey = item.attendanceDate != null ? item.attendanceDate : "Unknown Date";
            if (!grouped.containsKey(dateKey)) {
                grouped.put(dateKey, new ArrayList<>());
            }
            grouped.get(dateKey).add(item);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, List<Models.StudentAttendanceItem>> entry : grouped.entrySet()) {
            String dateStr = entry.getKey();
            List<Models.StudentAttendanceItem> dayItems = entry.getValue();

            // Date Header
            View headerView = inflater.inflate(R.layout.item_date_header, containerPeriods, false);
            TextView tvDate = headerView.findViewById(R.id.tv_date_header_title);
            TextView tvCount = headerView.findViewById(R.id.tv_date_header_summary);

            String formattedDate = dateStr;
            try {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US);
                Date parsed = parser.parse(dateStr);
                if (parsed != null) formattedDate = formatter.format(parsed);
            } catch (Exception ignored) {}

            tvDate.setText(formattedDate);
            int pCount = 0;
            for (Models.StudentAttendanceItem it : dayItems) {
                if (it.isPresent()) pCount++;
            }
            tvCount.setText(dayItems.size() + " Lectures • " + pCount + "P / " + (dayItems.size() - pCount) + "A");
            containerPeriods.addView(headerView);

            // Period Items
            for (Models.StudentAttendanceItem item : dayItems) {
                View periodView = inflater.inflate(R.layout.item_period, containerPeriods, false);

                View indicator = periodView.findViewById(R.id.view_period_indicator);
                TextView tvLectureNum = periodView.findViewById(R.id.tv_period_lecture_num);
                TextView tvPeriodDate = periodView.findViewById(R.id.tv_period_date);
                TextView tvStatusBadge = periodView.findViewById(R.id.tv_period_status_badge);
                TextView tvSubject = periodView.findViewById(R.id.tv_period_subject);
                TextView tvFaculty = periodView.findViewById(R.id.tv_period_faculty);
                TextView tvMarkedTime = periodView.findViewById(R.id.tv_period_marked_time);

                tvSubject.setText(item.getSubjectDisplayName());
                tvFaculty.setText("Faculty: " + item.getFacultyDisplayName());

                if (item.classLecture != null && !item.classLecture.isEmpty()) {
                    tvLectureNum.setText("LEC #" + item.classLecture);
                } else {
                    tvLectureNum.setText("PERIOD");
                }

                tvPeriodDate.setText(item.getFormattedDate());

                if (item.isPresent()) {
                    indicator.setBackgroundColor(Color.parseColor("#10B981"));
                    tvStatusBadge.setText("PRESENT");
                    tvStatusBadge.setTextColor(Color.parseColor("#34D399"));
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_tag_present);
                } else {
                    indicator.setBackgroundColor(Color.parseColor("#EF4444"));
                    tvStatusBadge.setText("ABSENT");
                    tvStatusBadge.setTextColor(Color.parseColor("#F87171"));
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_tag_absent);
                }

                if (item.createdAt != null && item.createdAt.contains("T")) {
                    try {
                        String timePart = item.createdAt.substring(item.createdAt.indexOf("T") + 1);
                        if (timePart.length() >= 5) {
                            tvMarkedTime.setText("Marked " + timePart.substring(0, 5));
                            tvMarkedTime.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception ignored) {}
                }

                containerPeriods.addView(periodView);
            }
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of Lloyd ERP?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    prefs.logout();
                    AttendanceWidgetProvider.triggerRefresh(this);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static int parseMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (glancePulseAnimator != null) {
            glancePulseAnimator.cancel();
        }
    }
}
