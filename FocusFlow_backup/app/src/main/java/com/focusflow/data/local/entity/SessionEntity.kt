package com.focusflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.focusflow.domain.model.Session
import com.focusflow.domain.model.SessionType
import java.util.*

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionType: String,
    val durationMs: Long,
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
    val isCompleted: Boolean = false,
    val treeId: Long? = null
) {
    companion object {
        fun fromDomain(session: Session): SessionEntity {
            return SessionEntity(
                id = session.id,
                sessionType = session.sessionType.name,
                durationMs = session.durationMs,
                startTimeMs = session.startTime.time,
                endTimeMs = session.endTime?.time,
                isCompleted = session.isCompleted,
                treeId = session.treeId
            )
        }
    }

    fun toDomain(): Session {
        return Session(
            id = id,
            sessionType = SessionType.valueOf(sessionType),
            durationMs = durationMs,
            startTime = Date(startTimeMs),
            endTime = endTimeMs?.let { Date(it) },
            isCompleted = isCompleted,
            treeId = treeId
        )
    }
}
