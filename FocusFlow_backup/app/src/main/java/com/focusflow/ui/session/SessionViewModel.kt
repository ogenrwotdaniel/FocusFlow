package com.focusflow.ui.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.focusflow.R
import com.focusflow.data.model.AudioTrack
import com.focusflow.data.model.AudioTracks
import com.focusflow.data.preference.UserPreferences
import com.focusflow.domain.model.Tree
import com.focusflow.service.timer.SessionType
import com.focusflow.service.timer.TimerInfo
import com.focusflow.service.tree.TreeGrowthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val treeGrowthService: TreeGrowthService
) : ViewModel() {

    private val _motivationalMessage = MutableLiveData<String>()
    val motivationalMessage: LiveData<String> = _motivationalMessage

    private val _currentTreeImage = MutableLiveData<Int?>()
    val currentTreeImage: LiveData<Int?> = _currentTreeImage
    
    private val _selectedAudioTrack = MutableStateFlow<AudioTrack?>(null)
    val selectedAudioTrack: StateFlow<AudioTrack?> = _selectedAudioTrack
    
    // Current volume level (0-100)
    private val _volume = MutableStateFlow(50)
    val volume: StateFlow<Int> = _volume
    
    // Current tree from the tree growth service
    private val _currentTree = MutableStateFlow<Tree?>(null)
    val currentTree: StateFlow<Tree?> = _currentTree

    // Motivational messages for focus sessions
    private val focusMotivations = listOf(
        "Stay focused on what matters most!",
        "Small steps lead to big achievements.",
        "Your future self will thank you for this focus session.",
        "Deep work builds deep knowledge.",
        "Quality focus time is the foundation of success.",
        "Concentration is the secret ingredient to excellence.",
        "Block out distractions, tune into your goals."
    )

    // Motivational messages for break sessions
    private val breakMotivations = listOf(
        "Rest is essential for peak performance.",
        "Take a moment to breathe and relax.",
        "Breaks help your brain process what you've learned.",
        "Enjoy this moment of calm.",
        "Recharge your mind for the next focus session.",
        "Step back, breathe, and return stronger."
    )

    // Tree growth stages
    private val treeStages = listOf(
        R.drawable.tree_seed,
        R.drawable.tree_sapling,
        R.drawable.tree_growing,
        R.drawable.tree_mature
    )

    init {
        loadFocusMotivation()
        showRandomTreeStage()
        
        // Load initial volume from preferences
        viewModelScope.launch {
            userPreferences.getVolume().collect {
                _volume.value = it
            }
        }
        
        // Load initial audio track from preferences
        viewModelScope.launch {
            val trackId = userPreferences.getAudioTrack().first()
            _selectedAudioTrack.value = AudioTracks.getById(trackId)
        }
        
        // Observe tree growth service
        viewModelScope.launch {
            treeGrowthService.currentTree.collect { tree ->
                _currentTree.value = tree
                updateTreeImage(tree)
            }
        }
    }

    fun loadFocusMotivation() {
        _motivationalMessage.value = focusMotivations.random()
    }

    fun loadBreakMotivation() {
        _motivationalMessage.value = breakMotivations.random()
    }
    
    /**
     * Update timer display and tree growth based on timer info
     */
    fun updateTimerState(timerInfo: TimerInfo) {
        // Update motivation based on session type
        if (timerInfo.sessionType == SessionType.FOCUS) {
            loadFocusMotivation()
        } else {
            loadBreakMotivation()
        }
        
        // Update tree growth
        viewModelScope.launch {
            treeGrowthService.updateTreeGrowth(timerInfo)
        }
    }
    
    /**
     * Start growing a new tree for a focus session
     */
    fun startTreeGrowth(sessionId: Long) {
        viewModelScope.launch {
            treeGrowthService.startGrowingTree(sessionId)
        }
    }
    
    /**
     * Mark the current tree as complete
     */
    fun completeTree() {
        viewModelScope.launch {
            treeGrowthService.completeTree()
        }
    }
    
    /**
     * Mark the current tree as withered (abandoned)
     */
    fun witherTree() {
        viewModelScope.launch {
            treeGrowthService.witherTree()
        }
    }

    private fun showRandomTreeStage() {
        // For now, just show a random tree stage
        // In a real implementation, this would be based on the session progress
        _currentTreeImage.value = treeStages[Random.nextInt(treeStages.size)]
    }
    
    /**
     * Update tree image based on current tree state
     */
    private fun updateTreeImage(tree: Tree?) {
        if (tree == null) {
            showRandomTreeStage()
            return
        }
        
        val imageRes = when (tree.growthStage) {
            com.focusflow.domain.model.GrowthStage.SEED -> R.drawable.tree_seed
            com.focusflow.domain.model.GrowthStage.SPROUT,
            com.focusflow.domain.model.GrowthStage.SAPLING -> R.drawable.tree_sapling
            com.focusflow.domain.model.GrowthStage.GROWING -> R.drawable.tree_growing
            com.focusflow.domain.model.GrowthStage.MATURE -> R.drawable.tree_mature
            else -> R.drawable.tree_seed
        }
        
        _currentTreeImage.value = imageRes
    }

    fun getCurrentVolume(): LiveData<Float> {
        val result = MutableLiveData<Float>()
        viewModelScope.launch {
            userPreferences.getAudioVolume().collect {
                result.value = it
            }
        }
        return result
    }
    
    /**
     * Get current volume as integer (0-100)
     */
    fun getVolumeAsInt(): LiveData<Int> {
        return userPreferences.getVolume().asLiveData()
    }

    fun saveVolume(volume: Float) {
        viewModelScope.launch {
            userPreferences.saveAudioVolume(volume)
        }
    }
    
    /**
     * Update volume as integer (0-100)
     */
    fun updateVolume(volumeLevel: Int) {
        viewModelScope.launch {
            userPreferences.updateVolume(volumeLevel)
            _volume.value = volumeLevel
        }
    }
    
    /**
     * Change the audio track
     */
    fun setAudioTrack(trackId: String) {
        viewModelScope.launch {
            userPreferences.saveAudioTrack(trackId)
            _selectedAudioTrack.value = AudioTracks.getById(trackId)
        }
    }
    
    /**
     * Get a list of all available audio tracks
     */
    fun getAvailableAudioTracks(): List<AudioTrack> {
        return AudioTracks.getAll()
    }
}
