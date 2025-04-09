package com.focusflow.data.remote

import android.net.Uri
import com.focusflow.domain.model.Tree
import com.focusflow.social.CommunityGarden
import com.focusflow.social.FriendChallenge
import com.focusflow.social.FriendConnection
import com.focusflow.social.GroupSession
import com.focusflow.social.SharedTree
import com.focusflow.social.SocialNotification
import com.focusflow.social.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository to manage all Firebase interactions for the app.
 * This handles authentication, Firestore database, real-time database,
 * and messaging. Storage operations have been replaced with default images.
 */
@Singleton
class FirebaseRepository @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val messaging = FirebaseMessaging.getInstance()

    // Collection references
    private val usersCollection = firestore.collection("users")
    private val gardensCollection = firestore.collection("gardens")
    private val sessionsCollection = firestore.collection("sessions")
    private val challengesCollection = firestore.collection("challenges")
    private val treesCollection = firestore.collection("trees")
    private val connectionsCollection = firestore.collection("connections")
    private val notificationsCollection = firestore.collection("notifications")

    // Default avatar URLs - using public URLs that don't require Firebase Storage
    private val defaultAvatars = listOf(
        "https://ui-avatars.com/api/?name=Focus&background=random",
        "https://ui-avatars.com/api/?name=Flow&background=random",
        "https://ui-avatars.com/api/?name=Productivity&background=random"
    )

    /**
     * Authentication Operations
     */
    suspend fun signInWithEmailPassword(email: String, password: String): FirebaseUser? {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        return authResult.user
    }

    suspend fun createAccountWithEmailPassword(email: String, password: String, displayName: String): FirebaseUser? {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        
        // Update profile with display name
        authResult.user?.let { user ->
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            
            user.updateProfile(profileUpdates).await()
            
            // Create initial user profile in Firestore
            val userProfile = UserProfile(
                userId = user.uid,
                displayName = displayName,
                email = email,
                joinDate = java.time.LocalDateTime.now()
            )
            
            usersCollection.document(user.uid).set(userProfile).await()
        }
        
        return authResult.user
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * User Profile Operations
     */
    suspend fun getUserProfile(userId: String = getCurrentUser()?.uid ?: ""): UserProfile? {
        if (userId.isEmpty()) return null
        
        val snapshot = usersCollection.document(userId).get().await()
        return snapshot.toObject<UserProfile>()
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean {
        return try {
            // Only allow updating the current user's profile
            val currentUserId = getCurrentUser()?.uid ?: return false
            if (profile.userId != currentUserId) return false
            
            usersCollection.document(currentUserId).set(profile, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Friend Connection Operations
     */
    suspend fun sendFriendRequest(targetUserId: String): FriendConnection? {
        val currentUserId = getCurrentUser()?.uid ?: return null
        
        // Check if connection already exists
        val existingConnections = connectionsCollection
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("friendId", targetUserId)
            .get()
            .await()
        
        if (!existingConnections.isEmpty) {
            return existingConnections.documents[0].toObject<FriendConnection>()
        }
        
        // Create new connection
        val connection = FriendConnection(
            userId = currentUserId,
            friendId = targetUserId
        )
        
        connectionsCollection.add(connection).await()
        
        // Create notification
        createFriendRequestNotification(targetUserId, currentUserId)
        
        return connection
    }

    suspend fun getUserFriendConnections(): Flow<List<FriendConnection>> = callbackFlow {
        val currentUserId = getCurrentUser()?.uid ?: ""
        if (currentUserId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Query connections where user is either the sender or receiver
        val query = connectionsCollection
            .whereEqualTo("userId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val connections = snapshot?.documents?.mapNotNull {
                it.toObject<FriendConnection>()
            } ?: emptyList()
            
            trySend(connections)
        }
        
        awaitClose { listener.remove() }
    }

    /**
     * Community Garden Operations
     */
    suspend fun createGarden(garden: CommunityGarden): CommunityGarden? {
        val currentUserId = getCurrentUser()?.uid ?: return null
        
        // Ensure the user is the owner
        val newGarden = garden.copy(ownerId = currentUserId)
        
        // Add to Firestore
        val ref = gardensCollection.add(newGarden).await()
        
        // Return the garden with the new ID
        return newGarden.copy(id = ref.id)
    }

    suspend fun getJoinedGardens(): Flow<List<CommunityGarden>> = callbackFlow {
        val currentUserId = getCurrentUser()?.uid ?: ""
        if (currentUserId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Query gardens where user is a member or owner
        val query = gardensCollection
            .whereArrayContains("memberIds", currentUserId)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val gardens = snapshot?.documents?.mapNotNull {
                it.toObject<CommunityGarden>()
            } ?: emptyList()
            
            trySend(gardens)
        }
        
        awaitClose { listener.remove() }
    }

    suspend fun addTreeToGarden(gardenId: String, tree: SharedTree): Boolean {
        return try {
            // Update garden document to add tree
            gardensCollection.document(gardenId).update(
                "trees", com.google.firebase.firestore.FieldValue.arrayUnion(tree)
            ).await()
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Group Session Operations
     */
    suspend fun createGroupSession(session: GroupSession): GroupSession? {
        val currentUserId = getCurrentUser()?.uid ?: return null
        
        // Ensure the user is the host
        val newSession = session.copy(hostId = currentUserId)
        
        // Add to Firestore
        val ref = sessionsCollection.add(newSession).await()
        
        // Return the session with the new ID
        return newSession.copy(id = ref.id)
    }

    suspend fun getActiveGroupSessions(): Flow<List<GroupSession>> = callbackFlow {
        val currentUserId = getCurrentUser()?.uid ?: ""
        if (currentUserId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Get the current time
        val now = java.time.LocalDateTime.now()
        
        // Query for active sessions where the user is a participant
        val query = sessionsCollection
            .whereArrayContains("participantIds", currentUserId)
            .whereGreaterThan("scheduledStartTime", now.minusHours(2))
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val sessions = snapshot?.documents?.mapNotNull {
                val session = it.toObject<GroupSession>()
                
                // Only include sessions that haven't ended
                if (session != null && !session.hasEnded()) {
                    session
                } else {
                    null
                }
            } ?: emptyList()
            
            trySend(sessions)
        }
        
        awaitClose { listener.remove() }
    }

    /**
     * Challenge Operations
     */
    suspend fun createChallenge(challenge: FriendChallenge): FriendChallenge? {
        val currentUserId = getCurrentUser()?.uid ?: return null
        
        // Ensure the user is the creator
        val newChallenge = challenge.copy(creatorId = currentUserId)
        
        // Add to Firestore
        val ref = challengesCollection.add(newChallenge).await()
        
        // Create notifications for participants
        challenge.participantIds.forEach { participantId ->
            if (participantId != currentUserId) {
                createChallengeInviteNotification(participantId, currentUserId, ref.id, challenge.name)
            }
        }
        
        // Return the challenge with the new ID
        return newChallenge.copy(id = ref.id)
    }

    /**
     * Notification Operations
     */
    private suspend fun createFriendRequestNotification(recipientId: String, senderId: String) {
        val senderProfile = getUserProfile(senderId)
        
        val notification = SocialNotification(
            recipientId = recipientId,
            senderId = senderId,
            type = com.focusflow.social.NotificationType.FRIEND_REQUEST,
            message = "${senderProfile?.displayName ?: "Someone"} sent you a friend request"
        )
        
        notificationsCollection.add(notification).await()
    }

    private suspend fun createChallengeInviteNotification(
        recipientId: String, 
        senderId: String,
        challengeId: String,
        challengeName: String
    ) {
        val senderProfile = getUserProfile(senderId)
        
        val notification = SocialNotification(
            recipientId = recipientId,
            senderId = senderId,
            type = com.focusflow.social.NotificationType.CHALLENGE_INVITE,
            message = "${senderProfile?.displayName ?: "Someone"} invited you to the challenge: $challengeName",
            challengeId = challengeId
        )
        
        notificationsCollection.add(notification).await()
    }

    suspend fun getUserNotifications(): Flow<List<SocialNotification>> = callbackFlow {
        val currentUserId = getCurrentUser()?.uid ?: ""
        if (currentUserId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val query = notificationsCollection
            .whereEqualTo("recipientId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val notifications = snapshot?.documents?.mapNotNull {
                it.toObject<SocialNotification>()
            } ?: emptyList()
            
            trySend(notifications)
        }
        
        awaitClose { listener.remove() }
    }

    /**
     * Analytics Operations
     */
    suspend fun updateUserStats(userId: String, stats: Map<String, Any>): Boolean {
        return try {
            usersCollection.document(userId)
                .collection("stats")
                .document("current")
                .set(stats, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateUserAnalytics(userId: String, analytics: Map<String, Any>): Boolean {
        return try {
            usersCollection.document(userId)
                .collection("analytics")
                .document("dashboard")
                .set(analytics, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getUserSessions(userId: String): List<com.focusflow.domain.model.FocusSession> {
        return try {
            val sessions = sessionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
                
            sessions.documents.mapNotNull { doc ->
                doc.toObject(com.focusflow.domain.model.FocusSession::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun recordInsightInteraction(userId: String, insightId: String): Boolean {
        return try {
            val interaction = hashMapOf(
                "insightId" to insightId,
                "timestamp" to System.currentTimeMillis(),
                "action" to "clicked"
            )
            
            usersCollection.document(userId)
                .collection("insightInteractions")
                .add(interaction)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Boolean {
        return try {
            usersCollection.document(userId)
                .collection("settings")
                .document("preferences")
                .set(settings, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Profile Image Operations
     * This method no longer uploads images to Firebase Storage.
     * Instead, it returns a generated avatar URL based on the user's name.
     */
    suspend fun uploadProfileImage(imageUri: Uri? = null): String {
        val currentUserId = getCurrentUser()?.uid ?: return defaultAvatars[0]
        val displayName = getCurrentUser()?.displayName ?: "User"
        
        // Generate a personalized avatar URL using the UI Avatars service
        val formattedName = displayName.replace(" ", "+")
        return "https://ui-avatars.com/api/?name=$formattedName&background=random&color=fff&size=200"
    }

    /**
     * FCM Messaging Operations
     */
    suspend fun updateFcmToken() {
        val currentUserId = getCurrentUser()?.uid ?: return
        
        try {
            val token = messaging.token.await()
            usersCollection.document(currentUserId)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            // Handle error
        }
    }
}
