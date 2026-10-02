package com.lloyd.attendance.ui;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lloyd.attendance.R;
import com.lloyd.attendance.api.ErpApiClient;
import com.lloyd.attendance.api.Models;
import com.lloyd.attendance.data.AppPreferences;
import com.lloyd.attendance.widget.AttendanceSyncWorker;
import com.lloyd.attendance.widget.AttendanceWidgetProvider;
import com.lloyd.attendance.widget.NotificationHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private TextView tvStudentName;
    private TextView tvStudentInfo;
    private TextView tvPercentage;
    private TextView tvBadge;
    private TextView tvStatPresent;
    private TextView tvStatAbsent;
    private TextView tvStatTotal;
    private TextView tvBunkAdvice;
    private ImageView ivBunkIcon;
    private TextView tvLastUpdated;

    // Navigation Tabs & Sections
    private Button tabOverview, tabClasses, tabSubjects, tabSimulator;
    private LinearLayout sectionOverview, sectionClasses, sectionSubjects, sectionSimulator;

    // Filter Controls in All Classes Log
    private Button filterAll, filterPresent, filterAbsent;
    private LinearLayout containerMonthChips;
    private LinearLayout containerSubjectChips;
    private TextView tvFilterSummary;
    private int currentStatusFilter = 0; // 0 = All, 1 = Present, 2 = Absent
    private String currentMonthFilter = "ALL";
    private String currentSubjectFilter = "ALL";

    // Attendance Log Records
    private final List<Models.StudentAttendanceItem> allAttendanceLogs = new ArrayList<>();

    // Dynamic Item Containers
    private LinearLayout containerMonthly;
    private LinearLayout containerPeriods;
    private LinearLayout containerSubjects;

    // Simulator
    private TextView tvSimProjectedPct, tvSimStatusNote, tvSimCurrentLabel;
    private SeekBar sbSimDelta;
    private int basePresent = 0;
    private int baseTotal = 0;

    // Action Buttons
    private ImageButton btnTestNotif, btnSync, btnLogout;
    private View cardWidgetGuide;

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

        // Schedule background WorkManager sync worker
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
        setupTabs();
        setupFilters();
        setupSimulator();

        swipeRefresh.setColorSchemeColors(Color.parseColor("#4F46E5"), Color.parseColor("#10B981"));
        swipeRefresh.setOnRefreshListener(this::fetchDataFromErp);

        btnSync.setOnClickListener(v -> {
            swipeRefresh.setRefreshing(true);
            fetchDataFromErp();
        });

        btnTestNotif.setOnClickListener(v -> triggerTestDynamicIslandAlert());
        btnLogout.setOnClickListener(v -> confirmLogout());
        cardWidgetGuide.setOnClickListener(v -> showWidgetGuideDialog());

        // Instantly render locally cached data so UI is fast and responsive
        renderCachedData();

        // Refresh with latest data from ERP
        swipeRefresh.setRefreshing(true);
        fetchDataFromErp();
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipe_refresh);
        tvStudentName = findViewById(R.id.tv_main_student_name);
        tvStudentInfo = findViewById(R.id.tv_main_student_info);
        tvPercentage = findViewById(R.id.tv_main_percentage);
        tvBadge = findViewById(R.id.tv_main_badge);
        tvStatPresent = findViewById(R.id.tv_stat_present);
        tvStatAbsent = findViewById(R.id.tv_stat_absent);
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvBunkAdvice = findViewById(R.id.tv_main_bunk_advice);
        ivBunkIcon = findViewById(R.id.iv_main_bunk_icon);
        tvLastUpdated = findViewById(R.id.tv_main_last_updated);

        // 4 Tabs
        tabOverview = findViewById(R.id.tab_overview);
        tabClasses = findViewById(R.id.tab_classes);
        tabSubjects = findViewById(R.id.tab_subjects);
        tabSimulator = findViewById(R.id.tab_simulator);

        // 4 Sections
        sectionOverview = findViewById(R.id.section_overview);
        sectionClasses = findViewById(R.id.section_classes);
        sectionSubjects = findViewById(R.id.section_subjects);
        sectionSimulator = findViewById(R.id.section_simulator);

        // Containers
        containerMonthly = findViewById(R.id.container_monthly_items);
        containerPeriods = findViewById(R.id.container_period_items);
        containerSubjects = findViewById(R.id.container_subject_items);

        // Filters in All Classes Log
        filterAll = findViewById(R.id.filter_all);
        filterPresent = findViewById(R.id.filter_present);
        filterAbsent = findViewById(R.id.filter_absent);
        containerMonthChips = findViewById(R.id.container_month_chips);
        containerSubjectChips = findViewById(R.id.container_subject_chips);
        tvFilterSummary = findViewById(R.id.tv_filter_summary);

        // Simulator
        tvSimProjectedPct = findViewById(R.id.tv_sim_projected_pct);
        tvSimStatusNote = findViewById(R.id.tv_sim_status_note);
        tvSimCurrentLabel = findViewById(R.id.tv_sim_current_label);
        sbSimDelta = findViewById(R.id.sb_sim_delta);

        // Actions
        btnTestNotif = findViewById(R.id.btn_main_test_notif);
        btnSync = findViewById(R.id.btn_main_sync);
        btnLogout = findViewById(R.id.btn_main_logout);
        cardWidgetGuide = findViewById(R.id.card_widget_guide);
    }

    private void setupTabs() {
        tabOverview.setOnClickListener(v -> selectTab(0));
        tabClasses.setOnClickListener(v -> selectTab(1));
        tabSubjects.setOnClickListener(v -> selectTab(2));
        tabSimulator.setOnClickListener(v -> selectTab(3));
    }

    private void selectTab(int index) {
        sectionOverview.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        sectionClasses.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        sectionSubjects.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        sectionSimulator.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        updateTabButton(tabOverview, index == 0);
        updateTabButton(tabClasses, index == 1);
        updateTabButton(tabSubjects, index == 2);
        updateTabButton(tabSimulator, index == 3);
    }

    private void updateTabButton(Button btn, boolean selected) {
        btn.setBackgroundResource(selected ? R.drawable.bg_button : R.drawable.bg_widget_pill);
        btn.setTextColor(selected ? Color.WHITE : Color.parseColor("#94A3B8"));
    }

    private void setupFilters() {
        filterAll.setOnClickListener(v -> {
            currentStatusFilter = 0;
            updateFilterTabsUI();
            applyFiltersAndRender();
        });
        filterPresent.setOnClickListener(v -> {
            currentStatusFilter = 1;
            updateFilterTabsUI();
            applyFiltersAndRender();
        });
        filterAbsent.setOnClickListener(v -> {
            currentStatusFilter = 2;
            updateFilterTabsUI();
            applyFiltersAndRender();
        });
    }

    private void updateFilterTabsUI() {
        if (currentStatusFilter == 0) {
            filterAll.setBackgroundResource(R.drawable.bg_button);
            filterAll.setTextColor(Color.WHITE);
            filterPresent.setBackgroundResource(R.drawable.bg_widget_pill);
            filterPresent.setTextColor(Color.parseColor("#34D399"));
            filterAbsent.setBackgroundResource(R.drawable.bg_widget_pill);
            filterAbsent.setTextColor(Color.parseColor("#F87171"));
        } else if (currentStatusFilter == 1) {
            filterAll.setBackgroundResource(R.drawable.bg_widget_pill);
            filterAll.setTextColor(Color.parseColor("#94A3B8"));
            filterPresent.setBackgroundResource(R.drawable.bg_button);
            filterPresent.setTextColor(Color.WHITE);
            filterAbsent.setBackgroundResource(R.drawable.bg_widget_pill);
            filterAbsent.setTextColor(Color.parseColor("#F87171"));
        } else {
            filterAll.setBackgroundResource(R.drawable.bg_widget_pill);
            filterAll.setTextColor(Color.parseColor("#94A3B8"));
            filterPresent.setBackgroundResource(R.drawable.bg_widget_pill);
            filterPresent.setTextColor(Color.parseColor("#34D399"));
            filterAbsent.setBackgroundResource(R.drawable.bg_button);
            filterAbsent.setTextColor(Color.WHITE);
        }
    }

    private void setupSimulator() {
        sbSimDelta.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int delta = progress - 10;
                updateSimulatorDisplay(delta);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateSimulatorDisplay(int delta) {
        if (delta > 0) {
            tvSimCurrentLabel.setText("+" + delta + " Attended");
            tvSimCurrentLabel.setTextColor(Color.parseColor("#34D399"));
        } else if (delta < 0) {
            tvSimCurrentLabel.setText(delta + " Missed");
            tvSimCurrentLabel.setTextColor(Color.parseColor("#F87171"));
        } else {
            tvSimCurrentLabel.setText("Current (0)");
            tvSimCurrentLabel.setTextColor(Color.WHITE);
        }

        int newP = basePresent;
        int newT = baseTotal;

        if (delta > 0) {
            newP += delta;
            newT += delta;
        } else if (delta < 0) {
            newT += Math.abs(delta);
        }

        double simPct = newT > 0 ? ((double) newP / newT) * 100.0 : 100.0;
        tvSimProjectedPct.setText(String.format(Locale.US, "%.1f%%", simPct));

        if (simPct >= 75.0) {
            tvSimProjectedPct.setTextColor(Color.parseColor("#10B981"));
            int canBunk = Math.max(0, (int) Math.floor((newP - 0.75 * newT) / 0.75));
            tvSimStatusNote.setText(String.format(Locale.US, "Safe! Can miss %d more classes after this.", canBunk));
        } else {
            tvSimProjectedPct.setTextColor(Color.parseColor("#EF4444"));
            int needed = Math.max(1, (int) Math.ceil((0.75 * newT - newP) / 0.25));
            tvSimStatusNote.setText(String.format(Locale.US, "Shortage! Will need %d consecutive classes to recover.", needed));
        }
    }

    private void triggerTestDynamicIslandAlert() {
        Models.CalculatedStats stats = prefs.getCachedStats();
        double pct = stats != null ? stats.overallPercentage : 85.0;

        NotificationHelper.showAttendanceMarkedNotification(
                this,
                "Fundamentals of Electronics Engineering",
                "Dr. Ridhima",
                "Lecture #6",
                true,
                pct
        );
        Toast.makeText(this, "Dynamic Island notification triggered in status bar!", Toast.LENGTH_SHORT).show();
    }

    private void renderCachedData() {
        Models.UserProfile user = prefs.getUserProfile();
        if (user != null && user.name != null && !user.name.isEmpty()) {
            tvStudentName.setText(user.name);
            String info = (user.course != null ? user.course : "") +
                    (user.semester != null ? " • Sem " + user.semester : "") +
                    (user.admission_no != null ? " • " + user.admission_no : "");
            tvStudentInfo.setText(info.isEmpty() ? "Lloyd College ERP" : info);
        } else {
            String savedUsername = prefs.getUsername();
            tvStudentName.setText(!savedUsername.isEmpty() ? savedUsername : "Lloyd Student");
            tvStudentInfo.setText("erp.lloydcollege.in");
        }

        Models.CalculatedStats cached = prefs.getCachedStats();
        if (cached != null) {
            updateStatsUI(cached);
        }

        String monthlyJson = prefs.getMonthlyData();
        if (monthlyJson != null) {
            try {
                Models.ApiResponse<Models.MonthlyAttendanceData> res = gson.fromJson(
                        monthlyJson, new TypeToken<Models.ApiResponse<Models.MonthlyAttendanceData>>(){}.getType()
                );
                if (res != null && res.data != null) populateMonthlyList(res.data);
            } catch (Exception ignored) {}
        }

        String logsJson = prefs.getAttendanceLogs();
        if (logsJson != null) {
            try {
                List<Models.StudentAttendanceItem> cachedLogs = gson.fromJson(
                        logsJson, new TypeToken<List<Models.StudentAttendanceItem>>(){}.getType()
                );
                if (cachedLogs != null && !cachedLogs.isEmpty()) {
                    updateAttendanceLogsData(cachedLogs);
                }
            } catch (Exception ignored) {}
        }
    }

    private void fetchDataFromErp() {
        executor.execute(() -> {
            try {
                // Ensure authorization token is valid
                apiClient.ensureValidToken();

                // Concurrently fetch Monthly Attendance, Weekly Timetable, and Complete Attendance Logs
                Future<Models.MonthlyAttendanceData> monthlyFuture = parallelExecutor.submit(() -> apiClient.getMonthlyAttendance());
                Future<List<Models.StudentAttendanceItem>> logsFuture = parallelExecutor.submit(() -> apiClient.getStudentAttendanceLogs(0));
                Future<Models.WeeklyAttendanceData> weeklyFuture = parallelExecutor.submit(() -> apiClient.getWeeklyAttendance());

                Models.MonthlyAttendanceData monthly = monthlyFuture.get();
                List<Models.StudentAttendanceItem> logs = logsFuture.get();
                weeklyFuture.get(); // Cache weekly routine

                Models.CalculatedStats stats = apiClient.calculateStatsFromMonthly(monthly);

                new Handler(Looper.getMainLooper()).post(() -> {
                    swipeRefresh.setRefreshing(false);
                    updateStatsUI(stats);
                    populateMonthlyList(monthly);
                    if (logs != null && !logs.isEmpty()) {
                        updateAttendanceLogsData(logs);
                    }
                    AttendanceWidgetProvider.triggerRefresh(MainActivity.this);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    swipeRefresh.setRefreshing(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "Sync incomplete";
                    Toast.makeText(MainActivity.this, "Sync: " + msg, Toast.LENGTH_SHORT).show();
                    renderCachedData();
                });
            }
        });
    }

    private void updateStatsUI(Models.CalculatedStats stats) {
        if (stats == null) return;

        if (stats.studentName != null && !stats.studentName.isEmpty()) {
            tvStudentName.setText(stats.studentName);
        }

        basePresent = stats.totalPresent;
        baseTotal = stats.totalClasses;

        tvPercentage.setText(String.format(Locale.US, "%.1f%%", stats.overallPercentage));
        tvStatPresent.setText(String.valueOf(stats.totalPresent));
        tvStatAbsent.setText(String.valueOf(stats.totalAbsent));
        tvStatTotal.setText(String.valueOf(stats.totalClasses));

        if (stats.overallPercentage >= 75.0) {
            tvPercentage.setTextColor(Color.parseColor("#10B981"));
            tvBadge.setText("SAFE (>= 75%)");
            tvBadge.setTextColor(Color.parseColor("#34D399"));
            ivBunkIcon.setImageResource(R.drawable.ic_check);

            if (stats.bunkAllowance > 0) {
                tvBunkAdvice.setText("You can safely bunk " + stats.bunkAllowance + " next class" + (stats.bunkAllowance > 1 ? "es" : "") + " and stay above 75%!");
            } else {
                tvBunkAdvice.setText("Attendance is right at 75%! Do not miss upcoming classes.");
            }
        } else {
            tvPercentage.setTextColor(Color.parseColor("#EF4444"));
            tvBadge.setText("SHORTAGE (< 75%)");
            tvBadge.setTextColor(Color.parseColor("#F87171"));
            ivBunkIcon.setImageResource(R.drawable.ic_alert);

            tvBunkAdvice.setText("You need to attend next " + stats.neededToReach75 + " class" + (stats.neededToReach75 > 1 ? "es" : "") + " consecutively to reach 75%!");
        }

        if (stats.lastUpdatedMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            tvLastUpdated.setText("Last Synced: " + sdf.format(new Date(stats.lastUpdatedMillis)));
        }

        sbSimDelta.setProgress(10);
        updateSimulatorDisplay(0);
    }

    private void populateMonthlyList(Models.MonthlyAttendanceData monthlyData) {
        if (monthlyData == null || monthlyData.months == null) return;

        containerMonthly.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Models.MonthItem m : monthlyData.months) {
            View itemView = inflater.inflate(R.layout.item_month, containerMonthly, false);

            TextView tvMonthName = itemView.findViewById(R.id.tv_item_month_name);
            TextView tvMonthPct = itemView.findViewById(R.id.tv_item_month_percentage);
            ProgressBar pbMonth = itemView.findViewById(R.id.pb_item_month);
            TextView tvClasses = itemView.findViewById(R.id.tv_item_month_classes);
            TextView tvStatus = itemView.findViewById(R.id.tv_item_month_status);

            tvMonthName.setText(m.monthLabel != null ? m.monthLabel : "Month " + m.monthNumber);
            tvMonthPct.setText(String.format(Locale.US, "%.1f%%", m.percentage));
            pbMonth.setProgress((int) Math.round(m.percentage));
            tvClasses.setText(m.present + " Present / " + m.total + " Total Classes");

            if (m.percentage >= 75.0) {
                tvMonthPct.setTextColor(Color.parseColor("#10B981"));
                pbMonth.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
                tvStatus.setText("Safe");
                tvStatus.setTextColor(Color.parseColor("#34D399"));
            } else {
                tvMonthPct.setTextColor(Color.parseColor("#EF4444"));
                pbMonth.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
                tvStatus.setText("Shortage");
                tvStatus.setTextColor(Color.parseColor("#F87171"));
            }

            containerMonthly.addView(itemView);
        }
    }

    private void updateAttendanceLogsData(List<Models.StudentAttendanceItem> logs) {
        allAttendanceLogs.clear();
        allAttendanceLogs.addAll(logs);

        buildMonthFilterChips();
        buildSubjectFilterChips();
        applyFiltersAndRender();
        renderSubjectWiseBreakdown();
    }

    private void buildMonthFilterChips() {
        containerMonthChips.removeAllViews();

        // Count occurrences per month (YYYY-MM)
        Map<String, Integer> monthCounts = new LinkedHashMap<>();
        for (Models.StudentAttendanceItem item : allAttendanceLogs) {
            if (item.attendanceDate != null && item.attendanceDate.length() >= 7) {
                String ym = item.attendanceDate.substring(0, 7);
                monthCounts.put(ym, monthCounts.getOrDefault(ym, 0) + 1);
            }
        }

        // 1. "All Dates" Chip
        boolean isAllSelected = "ALL".equals(currentMonthFilter);
        containerMonthChips.addView(createFilterChip("All Dates (" + allAttendanceLogs.size() + ")", isAllSelected, v -> {
            currentMonthFilter = "ALL";
            buildMonthFilterChips();
            applyFiltersAndRender();
        }));

        // 2. Individual Month Chips
        for (Map.Entry<String, Integer> entry : monthCounts.entrySet()) {
            String ym = entry.getKey();
            int count = entry.getValue();
            String label = formatMonthLabel(ym) + " (" + count + ")";
            boolean isSelected = ym.equals(currentMonthFilter);

            containerMonthChips.addView(createFilterChip(label, isSelected, v -> {
                currentMonthFilter = ym;
                buildMonthFilterChips();
                applyFiltersAndRender();
            }));
        }
    }

    private void buildSubjectFilterChips() {
        containerSubjectChips.removeAllViews();

        // Count occurrences per subject
        Map<String, Integer> subjectCounts = new LinkedHashMap<>();
        for (Models.StudentAttendanceItem item : allAttendanceLogs) {
            String sub = item.subjectName != null && !item.subjectName.isEmpty() ? item.subjectName : "General";
            subjectCounts.put(sub, subjectCounts.getOrDefault(sub, 0) + 1);
        }

        // 1. "All Subjects" Chip
        boolean isAllSelected = "ALL".equals(currentSubjectFilter);
        containerSubjectChips.addView(createFilterChip("All Subjects (" + allAttendanceLogs.size() + ")", isAllSelected, v -> {
            currentSubjectFilter = "ALL";
            buildSubjectFilterChips();
            applyFiltersAndRender();
        }));

        // 2. Individual Subject Chips
        for (Map.Entry<String, Integer> entry : subjectCounts.entrySet()) {
            String sub = entry.getKey();
            int count = entry.getValue();
            String shortName = getShortSubjectName(sub) + " (" + count + ")";
            boolean isSelected = sub.equals(currentSubjectFilter);

            containerSubjectChips.addView(createFilterChip(shortName, isSelected, v -> {
                currentSubjectFilter = sub;
                buildSubjectFilterChips();
                applyFiltersAndRender();
            }));
        }
    }

    private Button createFilterChip(String text, boolean isSelected, View.OnClickListener onClick) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        btn.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        btn.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#94A3B8"));
        int padH = dpToPx(12);
        int padV = dpToPx(6);
        btn.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(34)
        );
        lp.setMargins(0, 0, dpToPx(8), 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(onClick);
        return btn;
    }

    private void applyFiltersAndRender() {
        List<Models.StudentAttendanceItem> filtered = new ArrayList<>();
        int totalPresentInScope = 0;
        int totalAbsentInScope = 0;

        for (Models.StudentAttendanceItem item : allAttendanceLogs) {
            // Apply month filter
            if (!"ALL".equals(currentMonthFilter)) {
                if (item.attendanceDate == null || !item.attendanceDate.startsWith(currentMonthFilter)) {
                    continue;
                }
            }

            // Apply subject filter
            if (!"ALL".equals(currentSubjectFilter)) {
                if (item.subjectName == null || !item.subjectName.equals(currentSubjectFilter)) {
                    continue;
                }
            }

            boolean isPres = "Present".equalsIgnoreCase(item.status);
            if (isPres) totalPresentInScope++;
            else totalAbsentInScope++;

            // Apply status filter
            if (currentStatusFilter == 1 && !isPres) continue;
            if (currentStatusFilter == 2 && isPres) continue;

            filtered.add(item);
        }

        // Update status button badge counts
        filterAll.setText("All (" + (totalPresentInScope + totalAbsentInScope) + ")");
        filterPresent.setText("Present (" + totalPresentInScope + ")");
        filterAbsent.setText("Absent (" + totalAbsentInScope + ")");

        // Update summary text
        StringBuilder summary = new StringBuilder();
        summary.append("Showing ").append(filtered.size()).append(" of ").append(allAttendanceLogs.size()).append(" classes");
        if (!"ALL".equals(currentMonthFilter) || !"ALL".equals(currentSubjectFilter) || currentStatusFilter != 0) {
            summary.append(" • Filters active");
        }
        tvFilterSummary.setText(summary.toString());

        // Group filtered classes by Date
        Map<String, List<Models.StudentAttendanceItem>> groupedByDate = new LinkedHashMap<>();
        for (Models.StudentAttendanceItem item : filtered) {
            String d = item.attendanceDate != null ? item.attendanceDate : "Unknown Date";
            List<Models.StudentAttendanceItem> dayList = groupedByDate.computeIfAbsent(d, k -> new ArrayList<>());
            dayList.add(item);
        }

        containerPeriods.removeAllViews();
        if (filtered.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No lectures found matching the selected filter criteria.");
            emptyTv.setTextColor(Color.parseColor("#94A3B8"));
            emptyTv.setTextSize(13);
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(24, 60, 24, 60);
            containerPeriods.addView(emptyTv);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, List<Models.StudentAttendanceItem>> entry : groupedByDate.entrySet()) {
            String dateKey = entry.getKey();
            List<Models.StudentAttendanceItem> dayItems = entry.getValue();

            int dayPresent = 0;
            int dayAbsent = 0;
            for (Models.StudentAttendanceItem it : dayItems) {
                if ("Present".equalsIgnoreCase(it.status)) dayPresent++;
                else dayAbsent++;
            }

            // Inflate Date Group Header
            View headerView = inflater.inflate(R.layout.item_date_header, containerPeriods, false);
            TextView tvHeaderTitle = headerView.findViewById(R.id.tv_date_header_title);
            TextView tvHeaderSummary = headerView.findViewById(R.id.tv_date_header_summary);

            tvHeaderTitle.setText(formatDateForHeader(dateKey));
            tvHeaderSummary.setText(dayItems.size() + " Lectures • " + dayPresent + "P / " + dayAbsent + "A");
            containerPeriods.addView(headerView);

            // Inflate Period Cards for that Date
            for (Models.StudentAttendanceItem item : dayItems) {
                View cardView = inflater.inflate(R.layout.item_period, containerPeriods, false);

                View vIndicator = cardView.findViewById(R.id.view_period_indicator);
                TextView tvLec = cardView.findViewById(R.id.tv_period_lecture_num);
                TextView tvDate = cardView.findViewById(R.id.tv_period_date);
                TextView tvStatusBadge = cardView.findViewById(R.id.tv_period_status_badge);
                TextView tvSubject = cardView.findViewById(R.id.tv_period_subject);
                TextView tvFaculty = cardView.findViewById(R.id.tv_period_faculty);
                TextView tvTime = cardView.findViewById(R.id.tv_period_marked_time);

                boolean isPres = "Present".equalsIgnoreCase(item.status);
                vIndicator.setBackgroundColor(isPres ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

                tvLec.setText(item.classLecture != null && !item.classLecture.isEmpty() ? "LEC #" + item.classLecture : "CLASS");
                tvDate.setText(formatDateShort(dateKey));

                if (isPres) {
                    tvStatusBadge.setText("PRESENT");
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_tag_present);
                    tvStatusBadge.setTextColor(Color.parseColor("#34D399"));
                } else {
                    tvStatusBadge.setText("ABSENT");
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_tag_absent);
                    tvStatusBadge.setTextColor(Color.parseColor("#F87171"));
                }

                tvSubject.setText(item.subjectName != null && !item.subjectName.isEmpty() ? item.subjectName : "Lecture");
                tvFaculty.setText(item.createdByName != null && !item.createdByName.isEmpty() ? "Faculty: " + item.createdByName : "Faculty: Assigned Teacher");
                tvTime.setText(formatMarkedTime(item.createdAt));

                containerPeriods.addView(cardView);
            }
        }
    }

    private void renderSubjectWiseBreakdown() {
        containerSubjects.removeAllViews();
        if (allAttendanceLogs.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("Subject-wise stats will appear here as your timetable syncs.");
            emptyTv.setTextColor(Color.parseColor("#94A3B8"));
            emptyTv.setTextSize(13);
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(24, 48, 24, 48);
            containerSubjects.addView(emptyTv);
            return;
        }

        Map<String, Models.SubjectStat> map = new LinkedHashMap<>();
        for (Models.StudentAttendanceItem p : allAttendanceLogs) {
            String sub = p.subjectName != null && !p.subjectName.isEmpty() ? p.subjectName : "General Lecture";
            Models.SubjectStat stat = map.get(sub);
            if (stat == null) {
                stat = new Models.SubjectStat();
                stat.subjectName = sub;
                stat.teacherName = p.createdByName != null ? p.createdByName : "";
                map.put(sub, stat);
            }
            if (stat.teacherName.isEmpty() && p.createdByName != null && !p.createdByName.isEmpty()) {
                stat.teacherName = p.createdByName;
            }

            boolean isPres = "Present".equalsIgnoreCase(p.status);
            if (isPres) {
                stat.present++;
                stat.total++;
            } else {
                stat.absent++;
                stat.total++;
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Models.SubjectStat stat : map.values()) {
            if (stat.total > 0) {
                stat.percentage = Math.round(((double) stat.present / stat.total) * 1000.0) / 10.0;
            } else {
                stat.percentage = 100.0;
            }
            stat.isSafe = stat.percentage >= 75.0;

            View itemView = inflater.inflate(R.layout.item_subject, containerSubjects, false);

            TextView tvSubName = itemView.findViewById(R.id.tv_subject_name);
            TextView tvFaculty = itemView.findViewById(R.id.tv_subject_faculty);
            TextView tvPct = itemView.findViewById(R.id.tv_subject_percentage);
            ProgressBar pb = itemView.findViewById(R.id.pb_subject_progress);
            TextView tvStats = itemView.findViewById(R.id.tv_subject_stats);
            TextView tvBunk = itemView.findViewById(R.id.tv_subject_bunk);

            tvSubName.setText(stat.subjectName);
            tvFaculty.setText(!stat.teacherName.isEmpty() ? "Faculty: " + stat.teacherName : "Faculty: Assigned Teacher");
            tvPct.setText(String.format(Locale.US, "%.1f%%", stat.percentage));
            pb.setProgress((int) Math.round(stat.percentage));

            tvStats.setText(stat.present + " Attended • " + stat.absent + " Missed • " + stat.total + " Total");

            if (stat.isSafe) {
                tvPct.setTextColor(Color.parseColor("#10B981"));
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
                int canBunk = Math.max(0, (int) Math.floor((stat.present - 0.75 * stat.total) / 0.75));
                tvBunk.setText(canBunk > 0 ? "Can miss " + canBunk + " class" + (canBunk > 1 ? "es" : "") : "Safe (On 75% margin)");
                tvBunk.setTextColor(Color.parseColor("#34D399"));
            } else {
                tvPct.setTextColor(Color.parseColor("#EF4444"));
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
                int needed = Math.max(1, (int) Math.ceil((0.75 * stat.total - stat.present) / 0.25));
                tvBunk.setText("Need next " + needed + " class" + (needed > 1 ? "es" : ""));
                tvBunk.setTextColor(Color.parseColor("#F87171"));
            }

            containerSubjects.addView(itemView);
        }
    }

    private String formatDateForHeader(String yyyyMmDd) {
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = inFmt.parse(yyyyMmDd);
            if (d != null) {
                SimpleDateFormat outFmt = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US);
                return "📅 " + outFmt.format(d);
            }
        } catch (Exception ignored) {}
        return "📅 " + yyyyMmDd;
    }

    private String formatDateShort(String yyyyMmDd) {
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = inFmt.parse(yyyyMmDd);
            if (d != null) {
                SimpleDateFormat outFmt = new SimpleDateFormat("EEE, MMM dd", Locale.US);
                return outFmt.format(d);
            }
        } catch (Exception ignored) {}
        return yyyyMmDd;
    }

    private String formatMarkedTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            if (isoTime.contains("T")) {
                String timePart = isoTime.substring(isoTime.indexOf('T') + 1);
                if (timePart.length() >= 5) {
                    return "Marked " + timePart.substring(0, 5);
                }
            }
        } catch (Exception ignored) {}
        return "Marked";
    }

    private String formatMonthLabel(String ym) {
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM", Locale.US);
            Date d = inFmt.parse(ym);
            if (d != null) {
                SimpleDateFormat outFmt = new SimpleDateFormat("MMM yyyy", Locale.US);
                return outFmt.format(d);
            }
        } catch (Exception ignored) {}
        return ym;
    }

    private String getShortSubjectName(String name) {
        if (name == null) return "Class";
        if (name.length() <= 20) return name;
        if (name.contains("Electronics")) return "Electronics";
        if (name.contains("Chemistry")) return "Chemistry";
        if (name.contains("Programming")) return "Programming";
        if (name.contains("Mathematics")) return "Maths-I";
        if (name.contains("Sustainability")) return "Environment";
        if (name.contains("Communication")) return "Communication";
        if (name.contains("Knowledge")) return "Indian Knowledge";
        if (name.contains("AI & Prompt")) return "AI & Prompt";
        return name.substring(0, 18) + "…";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out? Your widget will stop syncing until you sign in again.")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    prefs.clearAll();
                    AttendanceWidgetProvider.triggerRefresh(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showWidgetGuideDialog() {
        new AlertDialog.Builder(this)
                .setTitle("How to Add the Widget")
                .setMessage("1. Go to your phone's Home Screen.\n\n" +
                        "2. Touch and hold any empty area.\n\n" +
                        "3. Tap 'Widgets'.\n\n" +
                        "4. Scroll down to 'Lloyd Attendance' and drag the widget to your home screen.\n\n" +
                        "5. Tap the widget anytime to refresh in the background!")
                .setPositiveButton("Got It", null)
                .show();
    }
}
