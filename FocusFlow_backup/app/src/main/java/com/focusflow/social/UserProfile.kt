package com.focusflow.social

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

/**
 * Represents a user profile in the FocusFlow app's social system.
 * Contains user information, achievements, and statistics.
 * Uses generated avatars instead of uploaded profile pictures.
 */
@Parcelize
data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String,
    val profileAvatarUrl: String = generateAvatarUrl(displayName),
    val bio: String = "",
    val joinDate: LocalDateTime,
    val isPublic: Boolean = true,
    val stats: UserStats = UserStats(),
    val preferences: SocialPreferences = SocialPreferences(),
    val achievements: List<Achievement> = emptyList(),
    val badges: List<String> = emptyList(),
    val lastActive: LocalDateTime = LocalDateTime.now()
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
 * User statistics for productivity and social engagement
 */
@Parcelize
data class UserStats(
    val totalFocusMinutes: Int = 0,
    val totalSessionsCompleted: Int = 0,
    val totalTreesGrown: Int = 0,
    val averageDailyFocusMinutes: Double = 0.0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val perfectDays: Int = 0,
    val groupSessionsJoined: Int = 0,
    val challengesCompleted: Int = 0,
    val gardensContributed: Int = 0,
    val friendsInspired: Int = 0,
    val productivityScore: Int = 0
) : Parcelable {
    /**
     * Calculate the user's focus level based on their statistics
     */
    fun calculateFocusLevel(): Int {
        // Simple algorithm to determine user's level based on total focus time and sessions
        val baseLevel = (totalFocusMinutes / 300) + (totalSessionsCompleted / 10)
        return (baseLevel + 1).coerceIn(1, 100)
    }
    
    /**
     * Get a description of the user's focus style based on their statistics
     */
    fun getFocusStyleDescription(): String {
        return when {
            averageDailyFocusMinutes >= 120 -> "Focus Master"
            averageDailyFocusMinutes >= 90 -> "Productivity Pro"
            averageDailyFocusMinutes >= 60 -> "Consistent Achiever"
            averageDailyFocusMinutes >= 30 -> "Growing Focuser"
            else -> "Focus Explorer"
        }
    }
}

/**
 * User preferences for social interactions
 */
@Parcelize
data class SocialPreferences(
    val allowFriendRequests: Boolean = true,
    val allowSessionInvites: Boolean = true,
    val showProgressToFriends: Boolean = true,
    val showInPublicLeaderboards: Boolean = false,
    val notifyForFriendActivity: Boolean = true,
    val shareNewAchievements: Boolean = true,
    val autoJoinFriendSessions: Boolean = false
) : Parcelable

/**
 * Represents an achievement earned by a user
 */
@Parcelize
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val earnedAt: LocalDateTime,
    val tier: AchievementTier = AchievementTier.BRONZE,
    val category: AchievementCategory,
    val isHidden: Boolean = false
) : Parcelable

/**
 * Tiers for achievements, representing different levels of difficulty
 */
enum class AchievementTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND
}

/**
 * Categories for achievements
 */
enum class AchievementCategory {
    FOCUS_TIME,
    SESSIONS_COMPLETED,
    TREES_GROWN,
    STREAKS,
    SOCIAL,
    CHALLENGES,
    SPECIAL
}
