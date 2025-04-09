package com.focusflow.social

import java.time.LocalDateTime
import java.util.UUID

/**
 * Manages user connections and friend relationships in the FocusFlow app.
 * Enables social accountability and collaborative focus sessions.
 */
data class FriendConnection(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val friendId: String,
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastInteractionAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Status of a friendship between two users
 */
enum class FriendshipStatus {
    PENDING,     // Friend request sent but not accepted
    ACCEPTED,    // Active friendship
    REJECTED,    // Friend request was rejected
    BLOCKED      // User has blocked this person
}

/**
 * Represents a friend challenge to boost motivation
 */
data class FriendChallenge(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val creatorId: String,
    val participantIds: List<String>,
    val targetFocusMinutes: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val status: ChallengeStatus = ChallengeStatus.CREATED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val rewards: List<String> = emptyList()
) {
    /**
     * Check if the challenge is currently active
     */
    fun isActive(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return currentTime.isAfter(startDate) && 
               currentTime.isBefore(endDate) && 
               status == ChallengeStatus.ACTIVE
    }
    
    /**
     * Calculate remaining days in the challenge
     */
    fun getRemainingDays(currentTime: LocalDateTime = LocalDateTime.now()): Int {
        if (currentTime.isAfter(endDate)) return 0
        return java.time.Duration.between(currentTime, endDate).toDays().toInt() + 1
    }
    
    /**
     * Calculate the current progress percentage
     */
    fun getTimeProgress(currentTime: LocalDateTime = LocalDateTime.now()): Int {
        if (currentTime.isBefore(startDate)) return 0
        if (currentTime.isAfter(endDate)) return 100
        
        val totalDuration = java.time.Duration.between(startDate, endDate)
        val elapsedDuration = java.time.Duration.between(startDate, currentTime)
        
        return ((elapsedDuration.toMillis().toDouble() / totalDuration.toMillis()) * 100).toInt()
            .coerceIn(0, 100)
    }
}

/**
 * Status of a friend challenge
 */
enum class ChallengeStatus {
    CREATED,     // Challenge created but not started
    ACTIVE,      // Challenge is currently active
    COMPLETED,   // Challenge has been completed
    FAILED,      // Challenge was not completed in time
    CANCELLED    // Challenge was cancelled
}

/**
 * Represents a friend's progress in a challenge
 */
data class ChallengeProgress(
    val userId: String,
    val challengeId: String,
    val minutesCompleted: Int = 0,
    val sessionsCompleted: Int = 0,
    val lastUpdateAt: LocalDateTime = LocalDateTime.now(),
    val achievements: List<String> = emptyList(),
    val rank: Int? = null
) {
    /**
     * Calculate percentage progress toward the challenge goal
     */
    fun getProgressPercentage(targetMinutes: Int): Int {
        return ((minutesCompleted.toDouble() / targetMinutes) * 100).toInt().coerceIn(0, 100)
    }
}

/**
 * Represents a social notification between friends
 */
data class SocialNotification(
    val id: String = UUID.randomUUID().toString(),
    val recipientId: String,
    val senderId: String? = null,
    val type: NotificationType,
    val message: String,
    val challengeId: String? = null,
    val gardenId: String? = null,
    val sessionId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isRead: Boolean = false,
    val actionUrl: String? = null
)

/**
 * Types of social notifications
 */
enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    CHALLENGE_INVITE,
    CHALLENGE_COMPLETED,
    GARDEN_INVITE,
    TREE_PLANTED,
    SESSION_INVITE,
    ACHIEVEMENT_UNLOCKED,
    GENERAL
}

/**
 * Measures engagement and encourages positive social interactions
 */
data class FriendInteraction(
    val userId: String,
    val friendId: String,
    val interactionType: InteractionType,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val associatedId: String? = null,
    val pointsAwarded: Int
)

/**
 * Types of interactions between friends
 */
enum class InteractionType {
    FOCUS_TOGETHER,
    ENCOURAGE,
    CHALLENGE_CREATE,
    CHALLENGE_COMPLETE,
    PLANT_TOGETHER,
    REACT_TO_TREE,
    MESSAGE
}
