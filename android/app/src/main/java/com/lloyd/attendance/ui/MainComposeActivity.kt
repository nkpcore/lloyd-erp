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
import androidx.core.content.ContextCompat
import com.lloyd.attendance.widget.AttendanceSyncWorker
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
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
import com.lloyd.attendance.core.designsystem.theme.LloydTheme
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.feature.dashboard.DashboardScreen
import com.lloyd.attendance.feature.dashboard.DashboardViewModel
import com.lloyd.attendance.feature.profile.ProfileScreen
import com.lloyd.attendance.feature.schedule.ScheduleScreen
import com.lloyd.attendance.feature.schedule.ScheduleViewModel
import com.lloyd.attendance.feature.simulation.SimulationScreen
import com.lloyd.attendance.feature.simulation.SimulationViewModel

enum class MainTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Attendance", Icons.Default.BarChart),
    SCHEDULE("Schedule", Icons.Default.CalendarToday),
    SIMULATOR("Simulator", Icons.Default.Calculate),
    PROFILE("Profile", Icons.Default.Person)
}

class MainComposeActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val scheduleViewModel: ScheduleViewModel by viewModels()
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
        prefs = AppPreferences(this)

        if (!prefs.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkNotificationPermission()
        AttendanceSyncWorker.schedulePeriodicSync(this)

        enableEdgeToEdge()

        setContent {
            LloydTheme {
                MainAppShell(
                    prefs = prefs,
                    dashboardViewModel = dashboardViewModel,
                    scheduleViewModel = scheduleViewModel,
                    simulationViewModel = simulationViewModel,
                    onLogout = {
                        prefs.tokenStore.clearTokens()
                        prefs.saveStudentId(0)
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
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
    scheduleViewModel: ScheduleViewModel,
    simulationViewModel: SimulationViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.DASHBOARD) }

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
                        onNavigateToSubjectSimulation = { subject: SubjectAttendance ->
                            simulationViewModel.initialize(
                                subjectName = subject.subjectName,
                                present = subject.presentCount,
                                total = subject.totalClasses,
                                code = subject.subjectCode,
                                teacher = subject.teacherName
                            )
                            selectedTab = MainTab.SIMULATOR
                        },
                        onNavigateToOverallSimulation = { p: Int, t: Int ->
                            simulationViewModel.initialize(
                                subjectName = "Overall Attendance",
                                present = p,
                                total = t
                            )
                            selectedTab = MainTab.SIMULATOR
                        },
                        onNavigateToSchedule = {
                            selectedTab = MainTab.SCHEDULE
                        }
                    )
                }

                MainTab.SCHEDULE -> {
                    ScheduleScreen(
                        viewModel = scheduleViewModel,
                        onNavigateBack = { selectedTab = MainTab.DASHBOARD }
                    )
                }

                MainTab.SIMULATOR -> {
                    SimulationScreen(
                        viewModel = simulationViewModel,
                        onNavigateBack = { selectedTab = MainTab.DASHBOARD }
                    )
                }

                MainTab.PROFILE -> {
                    ProfileScreen(
                        userProfile = prefs.userProfile,
                        studentId = prefs.getStudentId(),
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
