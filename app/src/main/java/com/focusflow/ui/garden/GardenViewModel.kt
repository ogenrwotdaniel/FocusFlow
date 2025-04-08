package com.focusflow.ui.garden

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.focusflow.data.repository.SessionRepository
import com.focusflow.data.repository.TreeRepository
import com.focusflow.domain.model.Tree
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val treeRepository: TreeRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val trees: LiveData<List<Tree>> = treeRepository.getAllTrees()
        .asLiveData(viewModelScope.coroutineContext)

    private val _gardenStats = MutableLiveData<GardenStats>()
    val gardenStats: LiveData<GardenStats> = _gardenStats

    init {
        loadGardenStats()
    }

    private fun loadGardenStats() {
        viewModelScope.launch {
            // Get total focus time
            sessionRepository.getTotalFocusTime().collect { totalTimeMs ->
                // Get tree count
                treeRepository.getTreeCount().collect { treeCount ->
                    _gardenStats.value = GardenStats(
                        totalTrees = treeCount,
                        totalFocusTimeMs = totalTimeMs,
                        totalFocusTimeFormatted = formatTime(totalTimeMs)
                    )
                }
            }
        }
    }

    private fun formatTime(timeMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
        
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}

data class GardenStats(
    val totalTrees: Int,
    val totalFocusTimeMs: Long,
    val totalFocusTimeFormatted: String
)
