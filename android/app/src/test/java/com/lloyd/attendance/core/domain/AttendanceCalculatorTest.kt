package com.lloyd.attendance.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun testZeroClassesReturnsNoData() {
        val pct = AttendancePercentage.of(0, 0)
        assertTrue(pct is AttendancePercentage.NoData)
        assertEquals("--.-%", pct.displayValue)
        assertNull(pct.numericValue)

        val needed75 = AttendanceCalculator.calculateClassesNeeded(0, 0, 75)
        assertEquals(0, needed75)

        val canBunk75 = AttendanceCalculator.calculateCanBunk(0, 0, 75)
        assertEquals(0, canBunk75)

        val advice = BunkAdvisor.getAdvice(0, 0)
        assertEquals("No Classes Recorded Yet", advice.headline)
        assertEquals(0, advice.canBunk75)
        assertEquals(0, advice.needed75)
    }

    @Test
    fun testZeroOutOfOneBoundary() {
        val pct = AttendancePercentage.of(0, 1)
        assertTrue(pct is AttendancePercentage.Value)
        assertEquals(0.0, (pct as AttendancePercentage.Value).percentage, 0.001)

        // At 0/1, to reach 75%, (0 + 3) / (1 + 3) = 3/4 = 75.0%
        val needed75 = AttendanceCalculator.calculateClassesNeeded(0, 1, 75)
        assertEquals(3, needed75)

        val canBunk75 = AttendanceCalculator.calculateCanBunk(0, 1, 75)
        assertEquals(0, canBunk75)

        val advice = BunkAdvisor.getAdvice(0, 1)
        assertFalse(advice.isSafe)
        assertEquals(3, advice.needed75)
    }

    @Test
    fun testOneOutOfOneBoundary() {
        val pct = AttendancePercentage.of(1, 1)
        assertTrue(pct is AttendancePercentage.Value)
        assertEquals(100.0, (pct as AttendancePercentage.Value).percentage, 0.001)

        val needed75 = AttendanceCalculator.calculateClassesNeeded(1, 1, 75)
        assertEquals(0, needed75)

        // If bunks 1 class, becomes 1/2 = 50.0% < 75%, so canBunk must be 0!
        val canBunk75 = AttendanceCalculator.calculateCanBunk(1, 1, 75)
        assertEquals(0, canBunk75)
    }

    @Test
    fun testSeventyFourOutOfOneHundredBoundary() {
        val pct = AttendancePercentage.of(74, 100)
        assertEquals(74.0, (pct as AttendancePercentage.Value).percentage, 0.001)

        // (74 + 4) / (100 + 4) = 78 / 104 = 75.0%
        val needed75 = AttendanceCalculator.calculateClassesNeeded(74, 100, 75)
        assertEquals(4, needed75)

        val canBunk75 = AttendanceCalculator.calculateCanBunk(74, 100, 75)
        assertEquals(0, canBunk75)
    }

    @Test
    fun testSeventyFiveOutOfOneHundredBoundary() {
        val pct = AttendancePercentage.of(75, 100)
        assertEquals(75.0, (pct as AttendancePercentage.Value).percentage, 0.001)

        val needed75 = AttendanceCalculator.calculateClassesNeeded(75, 100, 75)
        assertEquals(0, needed75)

        // If bunks 1 class, becomes 75/101 = 74.25% < 75%, so canBunk must be 0!
        val canBunk75 = AttendanceCalculator.calculateCanBunk(75, 100, 75)
        assertEquals(0, canBunk75)

        val advice = BunkAdvisor.getAdvice(75, 100)
        assertTrue(advice.isSafe)
        assertEquals("On the Line (75%)", advice.headline)
        assertEquals(0, advice.canBunk75)
    }

    @Test
    fun testThreeOutOfFourBoundary() {
        val pct = AttendancePercentage.of(3, 4)
        assertEquals(75.0, (pct as AttendancePercentage.Value).percentage, 0.001)

        val needed75 = AttendanceCalculator.calculateClassesNeeded(3, 4, 75)
        assertEquals(0, needed75)

        // If bunks 1 class, becomes 3/5 = 60.0% < 75%, so canBunk must be 0!
        val canBunk75 = AttendanceCalculator.calculateCanBunk(3, 4, 75)
        assertEquals(0, canBunk75)
    }

    @Test
    fun testFourOutOfFourBoundary() {
        val pct = AttendancePercentage.of(4, 4)
        assertEquals(100.0, (pct as AttendancePercentage.Value).percentage, 0.001)

        val needed75 = AttendanceCalculator.calculateClassesNeeded(4, 4, 75)
        assertEquals(0, needed75)

        // If bunks 1 class, becomes 4/5 = 80.0% >= 75%.
        // If bunks 2 classes, becomes 4/6 = 66.67% < 75%.
        // Thus canBunk must be exactly 1!
        val canBunk75 = AttendanceCalculator.calculateCanBunk(4, 4, 75)
        assertEquals(1, canBunk75)

        val advice = BunkAdvisor.getAdvice(4, 4)
        assertTrue(advice.isSafe)
        assertEquals("Safe: 1 Bunk(s) Available", advice.headline)
        assertEquals(1, advice.canBunk75)
    }

    @Test
    fun testMultiTargetThresholds() {
        val present = 82
        val total = 100
        val thresholds = AttendanceCalculator.calculateAllThresholds(present, total)

        // At 82/100:
        // For 75%: margin = 8200 - 7500 = 700. canBunk = 700 / 75 = 9.
        // Check: 82 / 109 = 75.22% >= 75%; 82 / 110 = 74.54% < 75%. Correct!
        assertEquals(9, thresholds[75]?.canBunkWhileMaintaining)
        assertEquals(0, thresholds[75]?.classesNeededToReach)

        // For 80%: margin = 8200 - 8000 = 200. canBunk = 200 / 80 = 2.
        // Check: 82 / 102 = 80.39% >= 80%; 82 / 103 = 79.61% < 80%. Correct!
        assertEquals(2, thresholds[80]?.canBunkWhileMaintaining)
        assertEquals(0, thresholds[80]?.classesNeededToReach)

        // For 85%: below target. numerator = 8500 - 8200 = 300.
        // needed = (300 + 14) / 15 = 314 / 15 = 20.
        // Check: (82 + 20) / (100 + 20) = 102 / 120 = 85.0%. Correct!
        assertEquals(0, thresholds[85]?.canBunkWhileMaintaining)
        assertEquals(20, thresholds[85]?.classesNeededToReach)

        // For 90%: below target. numerator = 9000 - 8200 = 800.
        // needed = (800 + 9) / 10 = 809 / 10 = 80.
        // Check: (82 + 80) / (100 + 80) = 162 / 180 = 90.0%. Correct!
        assertEquals(0, thresholds[90]?.canBunkWhileMaintaining)
        assertEquals(80, thresholds[90]?.classesNeededToReach)
    }

    @Test
    fun testSimulationEngine() {
        val sim = SimulationEngine.simulate(
            present = 20,
            total = 30,
            additionalPresent = 10,
            additionalAbsent = 2
        )

        assertEquals(20, sim.baselinePresent)
        assertEquals(30, sim.baselineTotal)
        assertEquals(30, sim.projectedPresent)
        assertEquals(42, sim.projectedTotal)

        // Base: 20/30 = 66.67%
        // Projected: 30/42 = 71.43%
        val proj = sim.projectedPercentage as AttendancePercentage.Value
        assertEquals(71.428, proj.percentage, 0.01)
        assertEquals(AttendanceHealth.BORDERLINE, sim.projectedHealth)
        assertTrue(sim.percentageDelta > 0)
    }
}
