package com.focusflow.domain.model

import java.util.*

data class Session(
    val id: Long = 0,
    val sessionType: SessionType,
    val durationMs: Long,
    val startTime: Date,
    val endTime: Date? = null,
    val isCompleted: Boolean = false,
    val treeId: Long? = null
) {
    val durationMinutes: Int
        get() = (durationMs / (1000 * 60)).toInt()

    companion object {
        /**
         * Creates a new focus session with the specified duration in minutes
         */
        fun createFocusSession(durationMinutes: Int): Session {
            return Session(
                sessionType = SessionType.FOCUS,
                durationMs = durationMinutes * 60 * 1000L,
                startTime = Date()
            )
        }

        /**
         * Creates a new break session with the specified duration in minutes
         */
        fun createBreakSession(durationMinutes: Int): Session {
            return Session(
                sessionType = SessionType.BREAK,
                durationMs = durationMinutes * 60 * 1000L,
                startTime = Date()
            )
        }
    }
}
