package com.lloyd.attendance.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.lloyd.attendance.core.designsystem.theme.LloydTheme
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.feature.dashboard.DashboardScreen
import com.lloyd.attendance.feature.dashboard.DashboardViewModel
import com.lloyd.attendance.feature.logs.AttendanceLogsScreen
import com.lloyd.attendance.feature.logs.AttendanceLogsViewModel
import com.lloyd.attendance.feature.profile.ProfileScreen
import com.lloyd.attendance.feature.simulation.SimulationScreen
import com.lloyd.attendance.feature.simulation.SimulationViewModel
import com.lloyd.attendance.feature.subject.SubjectDetailScreen
import com.lloyd.attendance.feature.subject.SubjectDetailViewModel
import com.lloyd.attendance.widget.AttendanceSyncWorker
import com.lloyd.attendance.widget.AttendanceWidgetProvider
import androidx.lifecycle.lifecycleScope
import com.lloyd.attendance.core.telemetry.TelemetryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.EventNote

enum class MainTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Attendance", Icons.Default.BarChart),
    LOGS("Daily Logs", Icons.AutoMirrored.Filled.EventNote),
    PROFILE("Profile", Icons.Default.Person)
}

class MainComposeActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val logsViewModel: AttendanceLogsViewModel by viewModels()
    private val subjectDetailViewModel: SubjectDetailViewModel by viewModels()
    private val simulationViewModel: SimulationViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences.getInstance(this)

        if (!prefs.isLoggedIn) {
            val loginIntent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(loginIntent)
            finish()
            return
        }

        checkNotificationPermission()
        AttendanceSyncWorker.schedulePeriodicSync(this)

        // Asynchronously report client telemetry if configured
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                TelemetryManager.sendTelemetry(
                    context = applicationContext,
                    endpointUrl = prefs.telemetryEndpoint,
                    studentId = prefs.getStudentId(),
                    studentName = prefs.userProfile?.name
                )
            } catch (ignored: Exception) {
            }
        }

        enableEdgeToEdge()
        setContent {
            LloydTheme {
                MainAppShell(
                    prefs = prefs,
                    dashboardViewModel = dashboardViewModel,
                    logsViewModel = logsViewModel,
                    subjectDetailViewModel = subjectDetailViewModel,
                    simulationViewModel = simulationViewModel,
                    onLogout = {
                        try {
                            WorkManager.getInstance(this).cancelAllWork()
                        } catch (ignored: Exception) {
                        }

                        prefs.logout()

                        try {
                            AttendanceWidgetProvider.updateAllWidgets(this, null, false, "Signed out")
                        } catch (ignored: Exception) {
                        }

                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finishAffinity()
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppShell(
    prefs: AppPreferences,
    dashboardViewModel: DashboardViewModel,
    logsViewModel: AttendanceLogsViewModel,
    subjectDetailViewModel: SubjectDetailViewModel,
    simulationViewModel: SimulationViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var selectedSubjectForDetail by remember { mutableStateOf<SubjectAttendance?>(null) }
    var showOverallSimulation by remember { mutableStateOf(false) }

    if (selectedSubjectForDetail != null) {
        SubjectDetailScreen(
            subject = selectedSubjectForDetail!!,
            onBack = { selectedSubjectForDetail = null },
            viewModel = subjectDetailViewModel
        )
    } else if (showOverallSimulation) {
        SimulationScreen(
            viewModel = simulationViewModel,
            onNavigateBack = { showOverallSimulation = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    MainTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToSubjectDetail = { subject: SubjectAttendance ->
                                selectedSubjectForDetail = subject
                            },
                            onNavigateToOverallSimulation = { p: Int, t: Int ->
                                simulationViewModel.initialize(
                                    subjectName = "Overall Attendance",
                                    present = p,
                                    total = t
                                )
                                showOverallSimulation = true
                            }
                        )
                    }

                    MainTab.LOGS -> {
                        AttendanceLogsScreen(
                            viewModel = logsViewModel
                        )
                    }

                    MainTab.PROFILE -> {
                        ProfileScreen(
                            userProfile = prefs.userProfile,
                            studentId = prefs.getStudentId(),
                            stats = prefs.cachedStats,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}
