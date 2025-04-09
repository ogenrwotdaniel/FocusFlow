package com.focusflow.core.synchronization

import android.content.Context
import androidx.room.withTransaction
import com.focusflow.data.local.AppDatabase
import com.focusflow.data.local.entity.SessionEntity
import com.focusflow.data.local.entity.TreeEntity
import com.focusflow.data.preference.UserPreferences
import com.focusflow.data.remote.FirebaseRepository
import com.focusflow.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages data synchronization between local and cloud storage.
 * Ensures that the user's focus data is available across all their devices.
 */
@Singleton
class DataSyncManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val firebaseRepository: FirebaseRepository,
    private val userPreferences: UserPreferences
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus = _syncStatus.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<LocalDateTime?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()
    
    /**
     * Initialize the sync manager and set up listeners for data changes
     */
    fun initialize() {
        scope.launch {
            // Load last sync time from preferences
            val lastSync = userPreferences.getLastSyncTime()
            _lastSyncTime.value = lastSync
            
            // Check if auto-sync is enabled
            val autoSyncEnabled = userPreferences.userData.first().syncEnabled
            if (autoSyncEnabled) {
                setupAutoSync()
            }
        }
    }
    
    /**
     * Manually sync data between local and cloud
     * @return true if sync was successful
     */
    suspend fun syncData(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            _syncStatus.value = SyncStatus.Syncing
            
            val isLoggedIn = firebaseRepository.getCurrentUser() != null
            if (!isLoggedIn) {
                _syncStatus.value = SyncStatus.Error("User not logged in")
                return@withContext false
            }
            
            // Step 1: Push local changes to cloud
            pushLocalDataToCloud()
            
            // Step 2: Pull remote changes to local
            pullRemoteDataToLocal()
            
            // Update sync time
            val now = LocalDateTime.now()
            _lastSyncTime.value = now
            userPreferences.saveLastSyncTime(now)
            
            _syncStatus.value = SyncStatus.Synced
            true
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error during sync")
            false
        }
    }
    
    /**
     * Enable or disable automatic synchronization
     */
    suspend fun setAutoSync(enabled: Boolean) {
        userPreferences.setSyncEnabled(enabled)
        
        if (enabled) {
            setupAutoSync()
        }
    }
    
    /**
     * Set up automatic synchronization based on data changes
     */
    private fun setupAutoSync() {
        // Listen for session changes
        scope.launch {
            database.sessionDao().getAllSessionsFlow().collect { sessions ->
                // Only sync if user is logged in
                if (firebaseRepository.getCurrentUser() != null) {
                    if (_syncStatus.value !is SyncStatus.Syncing) {
                        syncData()
                    }
                }
            }
        }
        
        // Listen for tree changes
        scope.launch {
            database.treeDao().getAllTreesFlow().collect { trees ->
                // Only sync if user is logged in
                if (firebaseRepository.getCurrentUser() != null) {
                    if (_syncStatus.value !is SyncStatus.Syncing) {
                        syncData()
                    }
                }
            }
        }
    }
    
    /**
     * Push local data to cloud storage
     */
    private suspend fun pushLocalDataToCloud() {
        // Get local sessions that need syncing
        val unSyncedSessions = database.sessionDao().getUnsyncedSessions()
        
        // Push each unsynchronized session to cloud
        unSyncedSessions.forEach { sessionEntity ->
            // Convert to cloud model and upload
            val cloudSession = CloudSession.fromEntity(sessionEntity)
            val uploadSuccessful = uploadSessionToCloud(cloudSession)
            
            if (uploadSuccessful) {
                // Mark as synced in local database
                database.sessionDao().updateSyncStatus(sessionEntity.id, true)
            }
        }
        
        // Get local trees that need syncing
        val unSyncedTrees = database.treeDao().getUnsyncedTrees()
        
        // Push each unsynchronized tree to cloud
        unSyncedTrees.forEach { treeEntity ->
            // Convert to cloud model and upload
            val cloudTree = CloudTree.fromEntity(treeEntity)
            val uploadSuccessful = uploadTreeToCloud(cloudTree)
            
            if (uploadSuccessful) {
                // Mark as synced in local database
                database.treeDao().updateSyncStatus(treeEntity.id, true)
            }
        }
    }
    
    /**
     * Pull remote data to local storage
     */
    private suspend fun pullRemoteDataToLocal() {
        // Get last sync time
        val lastSync = _lastSyncTime.value ?: LocalDateTime.MIN
        
        // Fetch remote sessions updated since last sync
        val remoteSessions = fetchRemoteSessionsSince(lastSync)
        
        // Update local database
        database.withTransaction {
            remoteSessions.forEach { cloudSession ->
                // Check if we have this session locally
                val localSession = database.sessionDao().getSessionById(cloudSession.id)
                
                if (localSession == null) {
                    // New session, insert
                    database.sessionDao().insert(cloudSession.toEntity())
                } else if (cloudSession.lastModified.isAfter(localSession.lastModifiedDate)) {
                    // Remote is newer, update
                    database.sessionDao().update(cloudSession.toEntity())
                }
                // If local is newer, no action (would have been pushed already)
            }
        }
        
        // Fetch remote trees updated since last sync
        val remoteTrees = fetchRemoteTreesSince(lastSync)
        
        // Update local database
        database.withTransaction {
            remoteTrees.forEach { cloudTree ->
                // Check if we have this tree locally
                val localTree = database.treeDao().getTreeById(cloudTree.id)
                
                if (localTree == null) {
                    // New tree, insert
                    database.treeDao().insert(cloudTree.toEntity())
                } else if (cloudTree.lastModified.isAfter(localTree.lastModifiedDate)) {
                    // Remote is newer, update
                    database.treeDao().update(cloudTree.toEntity())
                }
                // If local is newer, no action (would have been pushed already)
            }
        }
    }
    
    /**
     * Upload a session to cloud storage
     */
    private suspend fun uploadSessionToCloud(cloudSession: CloudSession): Boolean {
        return try {
            // Store in Firebase
            firebaseRepository.uploadSessionData(cloudSession)
            true
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Failed to upload session: ${e.message}")
            false
        }
    }
    
    /**
     * Upload a tree to cloud storage
     */
    private suspend fun uploadTreeToCloud(cloudTree: CloudTree): Boolean {
        return try {
            // Store in Firebase
            firebaseRepository.uploadTreeData(cloudTree)
            true
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Failed to upload tree: ${e.message}")
            false
        }
    }
    
    /**
     * Fetch remote sessions updated since a specific time
     */
    private suspend fun fetchRemoteSessionsSince(since: LocalDateTime): List<CloudSession> {
        return try {
            firebaseRepository.getSessionsUpdatedSince(since)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Failed to fetch sessions: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Fetch remote trees updated since a specific time
     */
    private suspend fun fetchRemoteTreesSince(since: LocalDateTime): List<CloudTree> {
        return try {
            firebaseRepository.getTreesUpdatedSince(since)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Failed to fetch trees: ${e.message}")
            emptyList()
        }
    }
}

/**
 * Cloud representation of a focus session
 */
data class CloudSession(
    val id: Long,
    val userId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val durationMinutes: Int,
    val completed: Boolean,
    val interrupted: Boolean,
    val interruptionCount: Int,
    val productivityScore: Double,
    val audioTrackId: String?,
    val notes: String?,
    val lastModified: LocalDateTime,
    val deviceId: String
) {
    /**
     * Convert to local database entity
     */
    fun toEntity(): SessionEntity {
        return SessionEntity(
            id = id,
            startTimeMs = startTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            endTimeMs = endTime?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000),
            durationMinutes = durationMinutes,
            completed = completed,
            interrupted = interrupted,
            interruptionCount = interruptionCount,
            productivityRating = productivityScore.toFloat(),
            audioTrackId = audioTrackId,
            notes = notes,
            synced = true,
            lastModifiedDate = lastModified,
            deviceId = deviceId
        )
    }
    
    companion object {
        /**
         * Create from local database entity
         */
        fun fromEntity(entity: SessionEntity): CloudSession {
            return CloudSession(
                id = entity.id,
                userId = "", // Will be set by Firebase repository
                startTime = LocalDateTime.ofEpochSecond(entity.startTimeMs / 1000, 0, java.time.ZoneOffset.UTC),
                endTime = entity.endTimeMs?.let { 
                    LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC) 
                },
                durationMinutes = entity.durationMinutes,
                completed = entity.completed,
                interrupted = entity.interrupted,
                interruptionCount = entity.interruptionCount,
                productivityScore = entity.productivityRating.toDouble(),
                audioTrackId = entity.audioTrackId,
                notes = entity.notes,
                lastModified = entity.lastModifiedDate,
                deviceId = entity.deviceId
            )
        }
    }
}

/**
 * Cloud representation of a tree
 */
data class CloudTree(
    val id: Long,
    val userId: String,
    val treeType: String,
    val growthStage: String,
    val plantedDate: LocalDateTime,
    val completedDate: LocalDateTime?,
    val sessionId: Long?,
    val lastModified: LocalDateTime,
    val deviceId: String
) {
    /**
     * Convert to local database entity
     */
    fun toEntity(): TreeEntity {
        return TreeEntity(
            id = id,
            treeType = treeType,
            growthStage = growthStage,
            plantedDateMs = plantedDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            completedDateMs = completedDate?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000),
            sessionId = sessionId,
            synced = true,
            lastModifiedDate = lastModified,
            deviceId = deviceId
        )
    }
    
    companion object {
        /**
         * Create from local database entity
         */
        fun fromEntity(entity: TreeEntity): CloudTree {
            return CloudTree(
                id = entity.id,
                userId = "", // Will be set by Firebase repository
                treeType = entity.treeType,
                growthStage = entity.growthStage,
                plantedDate = LocalDateTime.ofEpochSecond(entity.plantedDateMs / 1000, 0, java.time.ZoneOffset.UTC),
                completedDate = entity.completedDateMs?.let { 
                    LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC) 
                },
                sessionId = entity.sessionId,
                lastModified = entity.lastModifiedDate,
                deviceId = entity.deviceId
            )
        }
    }
}
