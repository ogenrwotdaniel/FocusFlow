package com.focusflow.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.focusflow.data.preference.UserPreferences
import com.focusflow.data.repository.SessionRepository
import com.focusflow.data.repository.TreeRepository
import com.focusflow.domain.model.Session
import com.focusflow.domain.model.Tree
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val treeRepository: TreeRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _todayFocusTime = MutableLiveData<String>()
    val todayFocusTime: LiveData<String> = _todayFocusTime

    private val _currentStreak = MutableLiveData<Int>()
    val currentStreak: LiveData<Int> = _currentStreak

    val recentSessions: LiveData<List<Session>> = sessionRepository.getRecentSessions(10)
        .asLiveData(viewModelScope.coroutineContext)

    val recentTrees: LiveData<List<Tree>> = treeRepository.getRecentTrees(3)
        .asLiveData(viewModelScope.coroutineContext)

    init {
        loadTodayFocusTime()
        calculateStreak()
    }

    private fun loadTodayFocusTime() {
        viewModelScope.launch {
            sessionRepository.getTodayTotalFocusTime().collect { totalMillis ->
                val hours = TimeUnit.MILLISECONDS.toHours(totalMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis) % 60
                
                _todayFocusTime.value = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
                }
            }
        }
    }

    private fun calculateStreak() {
        viewModelScope.launch {
            sessionRepository.getCurrentStreak().collect { streak ->
                _currentStreak.value = streak
            }
        }
    }

    fun getFocusSessionSettings() = userPreferences.getFocusSessionSettings()
}
