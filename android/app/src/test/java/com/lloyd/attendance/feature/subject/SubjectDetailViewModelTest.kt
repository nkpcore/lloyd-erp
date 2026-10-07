package com.lloyd.attendance.feature.subject

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectDetailViewModelTest {

    @Test
    fun testSubjectSimulationCalculation() {
        val present = 15
        val total = 20
        // Current: 75%
        // What-if attend 2 more: 17/22 = 77.27%
        val simulated = SubjectDetailViewModel.calculateSimulatedPercentage(
            currentPresent = present,
            currentTotal = total,
            classesToAttend = 2,
            classesToMiss = 0
        )
        assertEquals(77.27, simulated, 0.01)
    }

    @Test
    fun testSubjectSimulationCalculation_missClasses() {
        val present = 15
        val total = 20
        // What-if miss 2 classes: 15/22 = 68.18%
        val simulated = SubjectDetailViewModel.calculateSimulatedPercentage(
            currentPresent = present,
            currentTotal = total,
            classesToAttend = 0,
            classesToMiss = 2
        )
        assertEquals(68.18, simulated, 0.01)
    }
}
