package com.focusflow.social

import android.os.Parcelable
import com.focusflow.data.model.SessionType
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a collaborative focus session where multiple users can join and focus together.
 * This enables the social aspect of productivity with friend challenges and community support.
 */
@Parcelize
data class GroupSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostId: String,
    val participants: List<Participant> = emptyList(),
    val scheduledStartTime: LocalDateTime,
    val durationMinutes: Int,
    val sessionType: SessionType = SessionType.FOCUS,
    val isPrivate: Boolean = false,
    val inviteCode: String = generateInviteCode(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val description: String = "",
    val maxParticipants: Int = 10
) : Parcelable {

    /**
     * Check if the session is currently active
     */
    fun isActive(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        val endTime = scheduledStartTime.plusMinutes(durationMinutes.toLong())
        return currentTime.isAfter(scheduledStartTime) && currentTime.isBefore(endTime)
    }

    /**
     * Check if the session is upcoming (not started yet)
     */
    fun isUpcoming(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return currentTime.isBefore(scheduledStartTime)
    }

    /**
     * Check if the session has already ended
     */
    fun hasEnded(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        val endTime = scheduledStartTime.plusMinutes(durationMinutes.toLong())
        return currentTime.isAfter(endTime)
    }

    /**
     * Check if a user can join this session
     */
    fun canJoin(userId: String, currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return !hasEnded(currentTime) && 
               !isParticipant(userId) && 
               participants.size < maxParticipants
    }

    /**
     * Check if a user is already a participant in this session
     */
    fun isParticipant(userId: String): Boolean {
        return participants.any { it.userId == userId }
    }

    /**
     * Get the current progress of the session as a percentage (0-100)
     */
    fun getProgress(currentTime: LocalDateTime = LocalDateTime.now()): Int {
        if (isUpcoming(currentTime)) return 0
        if (hasEnded(currentTime)) return 100

        val totalDurationMinutes = durationMinutes.toLong()
        val elapsedMinutes = java.time.Duration.between(scheduledStartTime, currentTime).toMinutes()
        
        return ((elapsedMinutes.toDouble() / totalDurationMinutes) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Get the remaining time in minutes
     */
    fun getRemainingMinutes(currentTime: LocalDateTime = LocalDateTime.now()): Int {
        if (hasEnded(currentTime)) return 0
        if (isUpcoming(currentTime)) return durationMinutes

        val endTime = scheduledStartTime.plusMinutes(durationMinutes.toLong())
        return java.time.Duration.between(currentTime, endTime).toMinutes().toInt().coerceAtLeast(0)
    }

    companion object {
        /**
         * Generate a human-readable invite code
         */
        fun generateInviteCode(): String {
            val adjectives = listOf(
                "happy", "brave", "calm", "eager", "kind", "proud", "wise", "bold"
            )
            val nouns = listOf(
                "tiger", "eagle", "river", "mountain", "forest", "ocean", "planet", "galaxy"
            )
            
            val adjective = adjectives.random()
            val noun = nouns.random()
            val number = (100..999).random()
            
            return "$adjective-$noun-$number"
        }
    }
}

/**
 * Represents a participant in a group focus session
 */
@Parcelize
data class Participant(
    val userId: String,
    val displayName: String,
    val profileAvatarUrl: String = generateAvatarUrl(displayName),
    val joinedAt: LocalDateTime = LocalDateTime.now(),
    val isHost: Boolean = false,
    val status: ParticipantStatus = ParticipantStatus.JOINED,
    val completedPercentage: Int = 0
) : Parcelable {
    companion object {
        /**
         * Generates a URL for an avatar using the UI Avatars service
         * This eliminates the need for Firebase Storage
         */
        fun generateAvatarUrl(name: String): String {
            val formattedName = name.replace(" ", "+")
            return "https://ui-avatars.com/api/?name=$formattedName&background=random&color=fff&size=200"
        }
    }
}

/**
 * Possible states for a participant in a group session
 */
enum class ParticipantStatus {
    JOINED,       // Participant has joined but session hasn't started
    FOCUSING,     // Participant is actively focusing
    PAUSED,       // Participant has paused their timer
    COMPLETED,    // Participant has completed their focus session
    LEFT          // Participant has left the session before completion
}
