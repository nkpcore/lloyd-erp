package com.lloyd.attendance.feature.dashboard

import com.lloyd.attendance.core.data.DataSourceState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiStateTest {

    @Test
    fun `partial state sets isPartial true and isOffline false`() {
        val state = DashboardUiState(
            sourceState = DataSourceState.PARTIAL,
            errorMessage = "Attendance summary updated; class history could not refresh"
        )
        assertTrue(state.isPartial)
        assertFalse(state.isOffline)
    }

    @Test
    fun `offline and stale states set isOffline true and isPartial false`() {
        val offlineState = DashboardUiState(sourceState = DataSourceState.OFFLINE)
        val staleState = DashboardUiState(sourceState = DataSourceState.STALE)

        assertTrue(offlineState.isOffline)
        assertFalse(offlineState.isPartial)
        assertTrue(staleState.isOffline)
        assertFalse(staleState.isPartial)
    }

    @Test
    fun `fresh state sets both isOffline and isPartial false`() {
        val freshState = DashboardUiState(sourceState = DataSourceState.FRESH)
        assertFalse(freshState.isOffline)
        assertFalse(freshState.isPartial)
    }
}
