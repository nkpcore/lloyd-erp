package com.lloyd.attendance.campus.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * In-memory circular buffer preserving recent diagnostic events for real-time troubleshooting.
 */
class CampusDiagnosticsStore private constructor() {

    private val deque = ConcurrentLinkedDeque<CampusConnectionEvent>()
    private val _eventsFlow = MutableStateFlow<List<CampusConnectionEvent>>(emptyList())
    val eventsFlow: StateFlow<List<CampusConnectionEvent>> = _eventsFlow.asStateFlow()

    fun record(type: CampusConnectionEvent.Type, summary: String, details: String? = null) {
        val event = CampusConnectionEvent(
            type = type,
            summary = summary,
            details = details
        )
        deque.addFirst(event)
        while (deque.size > MAX_EVENTS) {
            deque.removeLast()
        }
        _eventsFlow.value = deque.toList()
    }

    fun clear() {
        deque.clear()
        _eventsFlow.value = emptyList()
    }

    companion object {
        private const val MAX_EVENTS = 50

        val instance: CampusDiagnosticsStore by lazy { CampusDiagnosticsStore() }
    }
}
