package com.lloyd.attendance.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.lloyd.attendance.core.security.BiometricAuthManager
import com.lloyd.attendance.core.security.BiometricCredentialVault
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.feature.dashboard.DashboardScreen
import com.lloyd.attendance.feature.dashboard.DashboardViewModel
import com.lloyd.attendance.core.access.AccessControlManager
import com.lloyd.attendance.core.access.AccessDecision
import com.lloyd.attendance.feature.lock.AccessLockoutScreen
import com.lloyd.attendance.core.ota.OtaUpdateManager
import com.lloyd.attendance.feature.lock.AppLockScreen
import com.lloyd.attendance.feature.logs.AttendanceLogsScreen
import com.lloyd.attendance.feature.logs.AttendanceLogsViewModel
import com.lloyd.attendance.feature.onboarding.WelcomeOnboardingSheet
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
import kotlinx.coroutines.withContext
import android.widget.Toast

enum class MainTab(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    DASHBOARD("Attendance", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    LOGS("Daily Logs", Icons.AutoMirrored.Filled.EventNote, Icons.AutoMirrored.Outlined.EventNote),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainComposeActivity : FragmentActivity() {

    private lateinit var prefs: AppPreferences
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val logsViewModel: AttendanceLogsViewModel by viewModels()
    private val subjectDetailViewModel: SubjectDetailViewModel by viewModels()
    private val simulationViewModel: SimulationViewModel by viewModels()

    private var isScreenOff = false
    private var isAuthenticating = false
    private var isAppLocked by mutableStateOf(false)
    private var accessDecision by mutableStateOf<AccessDecision?>(null)

    private fun performAccessCheck() {
        lifecycleScope.launch(Dispatchers.IO) {
            val studentId = prefs.getStudentId()
            val deviceId = TelemetryManager.getOrCreateDeviceId(applicationContext)

            val decision = AccessControlManager.checkAccess(
                context = applicationContext,
                endpointUrl = prefs.telemetryEndpoint,
                studentId = studentId,
                deviceId = deviceId
            )
            withContext(Dispatchers.Main) {
                accessDecision = decision
                if (decision is AccessDecision.Authorized) {
                    decision.broadcastNotice?.let { dashboardViewModel.setBroadcastNotice(it) }
                }
            }

            try {
                TelemetryManager.sendTelemetry(
                    context = applicationContext,
                    endpointUrl = prefs.telemetryEndpoint,
                    studentId = studentId,
                    studentName = prefs.userProfile?.name
                )
            } catch (ignored: Exception) {
            }

            // Also observe attendance snapshot to ensure real student name is reported once loaded
            val repository = com.lloyd.attendance.core.data.AttendanceRepository.getInstance(applicationContext)
            repository.snapshot.collect { snapshot ->
                val resolvedName = snapshot.overall.studentName
                if (resolvedName.isNotBlank() && resolvedName != prefs.userProfile?.name) {
                    try {
                        TelemetryManager.sendTelemetry(
                            context = applicationContext,
                            endpointUrl = prefs.telemetryEndpoint,
                            studentId = snapshot.overall.studentId,
                            studentName = resolvedName
                        )
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val vault = BiometricCredentialVault.getInstance(this@MainComposeActivity)
                if (prefs.isLoggedIn && vault.isAppLockEnabled() && BiometricAuthManager.isBiometricReady(this@MainComposeActivity)) {
                    isScreenOff = true
                    isAppLocked = true
                }
            }
        }
    }

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

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

        val screenFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, screenFilter)

        performAccessCheck()

        // Wire auto-logout on session expiration
        val repository = com.lloyd.attendance.core.data.AttendanceRepository.getInstance(applicationContext)
        lifecycleScope.launch {
            repository.sessionExpiredEvents.collect {
                withContext(Dispatchers.Main) {
                    handleAutoLogout("Session expired. Please sign in again.")
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            LloydTheme {
                if (isAppLocked) {
                    AppLockScreen(
                        studentName = prefs.userProfile?.name,
                        admissionNumber = prefs.username.ifBlank { if (prefs.studentId > 0) prefs.studentId.toString() else null },
                        onUnlockClick = {
                            promptAppLockAuthentication()
                        },
                        onSignOutClick = {
                            handleAutoLogout("Signed out")
                        }
                    )
                } else if (accessDecision != null && accessDecision !is AccessDecision.Authorized) {
                    val decision = accessDecision!!
                    val deviceId = remember { TelemetryManager.getOrCreateDeviceId(applicationContext) }
                    AccessLockoutScreen(
                        decision = decision,
                        deviceId = deviceId,
                        onRetry = {
                            performAccessCheck()
                        },
                        onUpdate = { url ->
                            OtaUpdateManager.downloadAndInstallApk(
                                this@MainComposeActivity,
                                url
                            )
                        },
                        onSignOut = {
                            handleAutoLogout("Signed out")
                        }
                    )
                } else {
                    MainAppShell(
                        prefs = prefs,
                        dashboardViewModel = dashboardViewModel,
                        logsViewModel = logsViewModel,
                        subjectDetailViewModel = subjectDetailViewModel,
                        simulationViewModel = simulationViewModel,
                        onLogout = {
                            handleAutoLogout("Signed out")
                        }
                    )

                    var showOnboarding by remember { mutableStateOf(!prefs.isOnboardingCompleted) }
                    if (showOnboarding) {
                        WelcomeOnboardingSheet(
                            onDismiss = {
                                prefs.isOnboardingCompleted = true
                                showOnboarding = false
                            },
                            onGetStarted = {
                                prefs.isOnboardingCompleted = true
                                showOnboarding = false
                                checkNotificationPermission()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isScreenOff && isAppLocked && !isAuthenticating) {
            promptAppLockAuthentication()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (ignored: Exception) {
        }
    }

    private fun promptAppLockAuthentication() {
        if (isAuthenticating) return
        isAuthenticating = true
        BiometricAuthManager.authenticate(
            activity = this,
            title = "Unlock Lloyd ERP",
            subtitle = "Confirm fingerprint to access attendance records",
            negativeButtonText = null, // Fallback to PIN / pattern / device password
            onSuccess = {
                isAuthenticating = false
                isScreenOff = false
                isAppLocked = false
            },
            onError = { _ ->
                isAuthenticating = false
                // Kept locked; user can tap "Unlock with Biometrics" anytime
            },
            onCancel = {
                isAuthenticating = false
                // Kept locked
            }
        )
    }

    private fun handleAutoLogout(reason: String = "Session expired. Please sign in again.") {
        try {
            WorkManager.getInstance(this).cancelAllWork()
        } catch (ignored: Exception) {
        }

        prefs.logout()

        try {
            AttendanceWidgetProvider.updateAllWidgets(this, null, false, "Session expired")
        } catch (ignored: Exception) {
        }

        Toast.makeText(this, reason, Toast.LENGTH_LONG).show()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("AUTH_ERROR", reason)
        }
        startActivity(intent)
        finishAffinity()
    }
}

sealed interface ScreenDestination {
    data object Main : ScreenDestination
    data class SubjectDetail(val subject: SubjectAttendance) : ScreenDestination
    data object OverallSimulation : ScreenDestination
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
    var destination by remember { mutableStateOf<ScreenDestination>(ScreenDestination.Main) }

    BackHandler(enabled = destination != ScreenDestination.Main) {
        destination = ScreenDestination.Main
    }

    val m3Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val m3Accelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            if (targetState is ScreenDestination.Main) {
                // Backward transition: Shared Z-Axis Zoom Out
                (fadeIn(animationSpec = tween(280, easing = m3Decelerate)) +
                 scaleIn(initialScale = 1.08f, animationSpec = tween(320, easing = m3Decelerate))) togetherWith
                (fadeOut(animationSpec = tween(200, easing = m3Accelerate)) +
                 scaleOut(targetScale = 0.90f, animationSpec = tween(240, easing = m3Accelerate)))
            } else {
                // Forward transition: Shared Z-Axis Zoom In
                (fadeIn(animationSpec = tween(280, easing = m3Decelerate)) +
                 scaleIn(initialScale = 0.90f, animationSpec = tween(320, easing = m3Decelerate))) togetherWith
                (fadeOut(animationSpec = tween(200, easing = m3Accelerate)) +
                 scaleOut(targetScale = 1.08f, animationSpec = tween(240, easing = m3Accelerate)))
            }
        },
        label = "screen_nav_transition"
    ) { currentDestination ->
        when (currentDestination) {
            is ScreenDestination.SubjectDetail -> {
                SubjectDetailScreen(
                    subject = currentDestination.subject,
                    onBack = { destination = ScreenDestination.Main },
                    viewModel = subjectDetailViewModel
                )
            }

            is ScreenDestination.OverallSimulation -> {
                SimulationScreen(
                    viewModel = simulationViewModel,
                    onNavigateBack = { destination = ScreenDestination.Main }
                )
            }

            is ScreenDestination.Main -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = { Text(tab.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(240, easing = m3Decelerate)) +
                                 scaleIn(initialScale = 0.96f, animationSpec = tween(240, easing = m3Decelerate))) togetherWith
                                (fadeOut(animationSpec = tween(160, easing = m3Accelerate)) +
                                 scaleOut(targetScale = 0.96f, animationSpec = tween(160, easing = m3Accelerate)))
                            },
                            label = "tab_animation"
                        ) { currentTab ->
                            when (currentTab) {
                                MainTab.DASHBOARD -> {
                                    DashboardScreen(
                                        viewModel = dashboardViewModel,
                                        onNavigateToSubjectDetail = { subject: SubjectAttendance ->
                                            destination = ScreenDestination.SubjectDetail(subject)
                                        },
                                        onNavigateToOverallSimulation = { p: Int, t: Int ->
                                            simulationViewModel.initialize(
                                                subjectName = "Overall Attendance",
                                                present = p,
                                                total = t
                                            )
                                            destination = ScreenDestination.OverallSimulation
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
        }
    }
}
