package com.focusflow.data.repository

import com.focusflow.data.local.dao.SessionDao
import com.focusflow.data.local.entity.SessionEntity
import com.focusflow.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    suspend fun createSession(session: Session): Long {
        return sessionDao.insertSession(SessionEntity.fromDomain(session))
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSession(SessionEntity.fromDomain(session))
    }

    suspend fun getSessionById(id: Long): Session? {
        return sessionDao.getSessionById(id)?.toDomain()
    }

    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getCompletedSessions(): Flow<List<Session>> {
        return sessionDao.getCompletedSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getCompletedFocusSessions(): Flow<List<Session>> {
        return sessionDao.getCompletedFocusSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getSessionsForDay(date: Date): Flow<List<Session>> {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return sessionDao.getSessionsForDay(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getTotalFocusTime(): Flow<Long> {
        return sessionDao.getTotalFocusTime().map { it ?: 0L }
    }

    fun getFocusTimeForDay(date: Date): Flow<Long> {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return sessionDao.getFocusTimeForDay(startOfDay, endOfDay).map { it ?: 0L }
    }

    fun getCompletedFocusSessionsCount(): Flow<Int> {
        return sessionDao.getCompletedFocusSessionsCount()
    }

    fun getLongestFocusSessionDuration(): Flow<Long> {
        return sessionDao.getLongestFocusSessionDuration().map { it ?: 0L }
    }
}
