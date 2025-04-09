package com.focusflow.service.tree

import com.focusflow.data.preference.UserPreferences
import com.focusflow.data.repository.TreeRepository
import com.focusflow.domain.model.GrowthStage
import com.focusflow.domain.model.Tree
import com.focusflow.domain.model.TreeType
import com.focusflow.service.timer.SessionType
import com.focusflow.service.timer.TimerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for tree growth during focus sessions.
 * Trees grow as the user completes focus sessions, with growth rate based on the timer progress.
 */
@Singleton
class TreeGrowthService @Inject constructor(
    private val treeRepository: TreeRepository,
    private val userPreferences: UserPreferences
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    // Current tree being grown in this session
    private val _currentTree = MutableStateFlow<Tree?>(null)
    val currentTree: StateFlow<Tree?> = _currentTree
    
    // Keep track of the last session ID to prevent duplicate trees
    private val lastSessionId = AtomicReference<Long?>(null)
    
    /**
     * Start growing a new tree for a focus session
     */
    suspend fun startGrowingTree(sessionId: Long) {
        // Only allow creating a tree for focus sessions
        if (lastSessionId.get() == sessionId) {
            return // Prevent duplicate trees for the same session
        }
        
        lastSessionId.set(sessionId)
        
        // Get user's preferred tree type
        val treeType = withContext(Dispatchers.IO) {
            TreeType.valueOf(userPreferences.getTreeType().first().uppercase())
        }
        
        // Create a new tree in SEED stage
        val newTree = Tree(
            treeType = treeType,
            growthStage = GrowthStage.SEED,
            sessionId = sessionId
        )
        
        // Save tree to database and update current tree
        coroutineScope.launch {
            val treeId = treeRepository.createTree(newTree)
            val savedTree = treeRepository.getTreeById(treeId)
            _currentTree.value = savedTree
        }
    }
    
    /**
     * Update tree growth based on timer progress
     */
    fun updateTreeGrowth(timerInfo: TimerInfo) {
        val tree = _currentTree.value ?: return
        
        // Only update tree growth for focus sessions
        if (timerInfo.sessionType != SessionType.FOCUS) {
            return
        }
        
        coroutineScope.launch {
            val updatedTree = tree.withUpdatedGrowthStage(timerInfo.progressPercent)
            
            if (tree.growthStage != updatedTree.growthStage) {
                // Only save to database when growth stage changes
                treeRepository.updateTree(updatedTree)
                _currentTree.value = updatedTree
            }
        }
    }
    
    /**
     * Mark the current tree as complete (fully grown)
     */
    fun completeTree() {
        val tree = _currentTree.value ?: return
        
        coroutineScope.launch {
            val completedTree = tree.completed()
            treeRepository.updateTree(completedTree)
            _currentTree.value = completedTree
        }
    }
    
    /**
     * Mark the current tree as withered (abandoned session)
     */
    fun witherTree() {
        val tree = _currentTree.value ?: return
        
        // Only wither trees that aren't fully grown yet
        if (tree.isFullyGrown) {
            return
        }
        
        coroutineScope.launch {
            val witheredTree = tree.withered()
            treeRepository.updateTree(witheredTree)
            _currentTree.value = witheredTree
        }
    }
    
    /**
     * Reset the current tree (called when sessions end or are cancelled)
     */
    fun reset() {
        _currentTree.value = null
        lastSessionId.set(null)
    }
    
    /**
     * Get the total number of fully grown trees
     */
    fun getFullyGrownTreeCount(): Flow<Int> {
        return treeRepository.getFullyGrownTreeCount()
    }
}
