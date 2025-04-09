package com.focusflow.domain.model

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

enum class SessionType {
    FOCUS,
    BREAK
}

data class TimerInfo(
    val sessionType: SessionType = SessionType.FOCUS,
    val state: TimerState = TimerState.IDLE,
    val totalDurationMs: Long = 0,
    val remainingTimeMs: Long = 0,
    val elapsedTimeMs: Long = 0,
    val progressPercent: Float = 0f,
    val currentSessionId: Long = 0
) {
    fun isRunning() = state == TimerState.RUNNING
    fun isPaused() = state == TimerState.PAUSED
    fun isFinished() = state == TimerState.FINISHED
    fun isActive() = state == TimerState.RUNNING || state == TimerState.PAUSED
    fun isFocusSession() = sessionType == SessionType.FOCUS
    fun isBreakSession() = sessionType == SessionType.BREAK
}
