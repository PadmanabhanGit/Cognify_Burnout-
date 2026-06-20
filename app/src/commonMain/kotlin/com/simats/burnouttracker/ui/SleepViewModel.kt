package com.simats.burnouttracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.burnouttracker.data.SleepRepository
import com.simats.burnouttracker.data.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SleepViewModel(private val repository: SleepRepository) : ViewModel() {
    private val _sessions = MutableStateFlow<List<SleepSessionData>>(emptyList())
    val sessions: StateFlow<List<SleepSessionData>> = _sessions.asStateFlow()

    private val _selectedSessionLogs = MutableStateFlow<List<AppUsageLogData>>(emptyList())
    val selectedSessionLogs: StateFlow<List<AppUsageLogData>> = _selectedSessionLogs.asStateFlow()

    private val _selectedWakeEvents = MutableStateFlow<List<WakeEventData>>(emptyList())
    val selectedWakeEvents: StateFlow<List<WakeEventData>> = _selectedWakeEvents.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            repository.getRecentSessions().collect {
                _sessions.value = it
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.refreshSleepData()
        }
    }

    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            _selectedSessionLogs.value = repository.getUsageLogs(sessionId)
            _selectedWakeEvents.value = repository.getWakeEvents(sessionId)
        }
    }
}
