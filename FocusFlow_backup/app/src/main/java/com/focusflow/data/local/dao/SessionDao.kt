package com.focusflow.data.local.dao

import androidx.room.*
import com.focusflow.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startTimeMs DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isCompleted = 1 ORDER BY startTimeMs DESC")
    fun getCompletedSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE sessionType = 'FOCUS' AND isCompleted = 1 ORDER BY startTimeMs DESC")
    fun getCompletedFocusSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTimeMs >= :startOfDay AND startTimeMs < :endOfDay ORDER BY startTimeMs DESC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<SessionEntity>>

    @Query("SELECT SUM(durationMs) FROM sessions WHERE sessionType = 'FOCUS' AND isCompleted = 1")
    fun getTotalFocusTime(): Flow<Long?>

    @Query("SELECT SUM(durationMs) FROM sessions WHERE sessionType = 'FOCUS' AND isCompleted = 1 AND startTimeMs >= :startOfDay AND startTimeMs < :endOfDay")
    fun getFocusTimeForDay(startOfDay: Long, endOfDay: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM sessions WHERE sessionType = 'FOCUS' AND isCompleted = 1")
    fun getCompletedFocusSessionsCount(): Flow<Int>

    @Query("SELECT MAX(durationMs) FROM sessions WHERE sessionType = 'FOCUS' AND isCompleted = 1")
    fun getLongestFocusSessionDuration(): Flow<Long?>
}
