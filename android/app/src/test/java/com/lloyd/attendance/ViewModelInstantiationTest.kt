package com.lloyd.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.lloyd.attendance.feature.calendar.AttendanceCalendarViewModel
import com.lloyd.attendance.feature.dashboard.DashboardViewModel
import com.lloyd.attendance.feature.feedback.FeedbackViewModel
import com.lloyd.attendance.feature.latest.LatestAttendanceViewModel
import com.lloyd.attendance.feature.logs.AttendanceLogsViewModel
import com.lloyd.attendance.feature.receipts.FeeReceiptsViewModel
import com.lloyd.attendance.feature.simulation.SimulationViewModel
import com.lloyd.attendance.feature.subject.SubjectDetailViewModel
import com.lloyd.attendance.feature.syllabus.SyllabusViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test

class ViewModelInstantiationTest {

    @Test
    fun testAllAndroidViewModelsHaveApplicationConstructor() {
        val androidViewModels: List<Class<out AndroidViewModel>> = listOf(
            DashboardViewModel::class.java,
            AttendanceLogsViewModel::class.java,
            SubjectDetailViewModel::class.java,
            AttendanceCalendarViewModel::class.java,
            LatestAttendanceViewModel::class.java,
            SyllabusViewModel::class.java,
            FeedbackViewModel::class.java,
            FeeReceiptsViewModel::class.java
        )

        for (vmClass in androidViewModels) {
            val ctor = try {
                vmClass.getConstructor(Application::class.java)
            } catch (e: NoSuchMethodException) {
                null
            }
            assertNotNull(
                "ViewModel ${vmClass.simpleName} must have a single-argument Constructor(Application) for ViewModelProvider compatibility",
                ctor
            )
        }
    }

    @Test
    fun testZeroArgViewModelsHaveDefaultConstructor() {
        val zeroArgViewModels: List<Class<out ViewModel>> = listOf(
            SimulationViewModel::class.java
        )

        for (vmClass in zeroArgViewModels) {
            val ctor = try {
                vmClass.getConstructor()
            } catch (e: NoSuchMethodException) {
                null
            }
            assertNotNull(
                "ViewModel ${vmClass.simpleName} must have a zero-argument default constructor",
                ctor
            )
        }
    }
}
