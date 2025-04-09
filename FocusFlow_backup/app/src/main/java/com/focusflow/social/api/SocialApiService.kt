package com.focusflow.social.api

import com.focusflow.social.ChallengeProgress
import com.focusflow.social.CommunityGarden
import com.focusflow.social.FriendChallenge
import com.focusflow.social.FriendConnection
import com.focusflow.social.GroupSession
import com.focusflow.social.Participant
import com.focusflow.social.SharedTree
import com.focusflow.social.SocialNotification
import com.focusflow.social.UserProfile
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API service interface for social functionality.
 * Handles communication with the backend for friend connections,
 * group sessions, community gardens, and challenges.
 */
interface SocialApiService {
    /**
     * User Profile Endpoints
     */
    @GET("users/profile")
    suspend fun getCurrentUserProfile(): UserProfile
    
    @GET("users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): UserProfile
    
    @PUT("users/profile")
    suspend fun updateUserProfile(@Body profile: UserProfile): UserProfile
    
    /**
     * Friend System Endpoints
     */
    @GET("friends")
    suspend fun getFriendConnections(): List<FriendConnection>
    
    @POST("friends/request/{userId}")
    suspend fun sendFriendRequest(@Path("userId") userId: String): FriendConnection
    
    @PUT("friends/accept/{connectionId}")
    suspend fun acceptFriendRequest(@Path("connectionId") connectionId: String): FriendConnection
    
    @PUT("friends/reject/{connectionId}")
    suspend fun rejectFriendRequest(@Path("connectionId") connectionId: String): FriendConnection
    
    @DELETE("friends/{connectionId}")
    suspend fun removeFriend(@Path("connectionId") connectionId: String)
    
    /**
     * Group Session Endpoints
     */
    @GET("sessions/groups")
    suspend fun getJoinedGroupSessions(
        @Query("status") status: String? = null, // upcoming, active, completed
        @Query("limit") limit: Int = 10
    ): List<GroupSession>
    
    @GET("sessions/groups/public")
    suspend fun getPublicGroupSessions(
        @Query("limit") limit: Int = 10
    ): List<GroupSession>
    
    @POST("sessions/groups")
    suspend fun createGroupSession(@Body session: GroupSession): GroupSession
    
    @GET("sessions/groups/{sessionId}")
    suspend fun getGroupSession(@Path("sessionId") sessionId: String): GroupSession
    
    @POST("sessions/groups/{sessionId}/join")
    suspend fun joinGroupSession(
        @Path("sessionId") sessionId: String, 
        @Query("inviteCode") inviteCode: String? = null
    ): GroupSession
    
    @POST("sessions/groups/{sessionId}/leave")
    suspend fun leaveGroupSession(@Path("sessionId") sessionId: String): GroupSession
    
    @PUT("sessions/groups/{sessionId}/status")
    suspend fun updateParticipantStatus(
        @Path("sessionId") sessionId: String,
        @Body participant: Participant
    ): GroupSession
    
    /**
     * Community Garden Endpoints
     */
    @GET("gardens")
    suspend fun getJoinedGardens(): List<CommunityGarden>
    
    @GET("gardens/public")
    suspend fun getPublicGardens(
        @Query("limit") limit: Int = 10
    ): List<CommunityGarden>
    
    @POST("gardens")
    suspend fun createGarden(@Body garden: CommunityGarden): CommunityGarden
    
    @GET("gardens/{gardenId}")
    suspend fun getGarden(@Path("gardenId") gardenId: String): CommunityGarden
    
    @POST("gardens/{gardenId}/join")
    suspend fun joinGarden(@Path("gardenId") gardenId: String): CommunityGarden
    
    @POST("gardens/{gardenId}/leave")
    suspend fun leaveGarden(@Path("gardenId") gardenId: String)
    
    @POST("gardens/{gardenId}/trees")
    suspend fun addTreeToGarden(
        @Path("gardenId") gardenId: String, 
        @Body tree: SharedTree
    ): CommunityGarden
    
    @POST("gardens/{gardenId}/trees/{treeId}/react")
    suspend fun reactToTree(
        @Path("gardenId") gardenId: String,
        @Path("treeId") treeId: String,
        @Query("reactionType") reactionType: String
    ): SharedTree
    
    /**
     * Challenge Endpoints
     */
    @GET("challenges")
    suspend fun getChallenges(
        @Query("status") status: String? = null // active, completed, all
    ): List<FriendChallenge>
    
    @POST("challenges")
    suspend fun createChallenge(@Body challenge: FriendChallenge): FriendChallenge
    
    @GET("challenges/{challengeId}")
    suspend fun getChallenge(@Path("challengeId") challengeId: String): FriendChallenge
    
    @POST("challenges/{challengeId}/join")
    suspend fun joinChallenge(@Path("challengeId") challengeId: String): FriendChallenge
    
    @POST("challenges/{challengeId}/leave")
    suspend fun leaveChallenge(@Path("challengeId") challengeId: String)
    
    @GET("challenges/{challengeId}/progress")
    suspend fun getChallengeProgress(@Path("challengeId") challengeId: String): List<ChallengeProgress>
    
    @PUT("challenges/{challengeId}/progress")
    suspend fun updateChallengeProgress(
        @Path("challengeId") challengeId: String,
        @Body progress: ChallengeProgress
    ): ChallengeProgress
    
    /**
     * Notification Endpoints
     */
    @GET("notifications")
    suspend fun getNotifications(@Query("unreadOnly") unreadOnly: Boolean = false): List<SocialNotification>
    
    @PUT("notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: String): SocialNotification
    
    @PUT("notifications/read-all")
    suspend fun markAllNotificationsAsRead()
}
