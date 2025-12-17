package com.example.lab4.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab4.data.model.*
import com.example.lab4.data.repository.ScheduleRepository
import com.example.lab4.data.repository.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for schedule operations
 * Shared by HomeFragment, CreateScheduleFragment, EditScheduleFragment, and ScheduleDetailsFragment
 */
class ScheduleViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _schedulesState = MutableStateFlow<UiState<List<ScheduleResponseDto>>>(UiState.Idle)
    val schedulesState: StateFlow<UiState<List<ScheduleResponseDto>>> = _schedulesState.asStateFlow()

    private val _scheduleDetailState = MutableStateFlow<UiState<ScheduleResponseDto>>(UiState.Idle)
    val scheduleDetailState: StateFlow<UiState<ScheduleResponseDto>> = _scheduleDetailState.asStateFlow()

    private val _createScheduleState = MutableStateFlow<UiState<ScheduleResponseDto>>(UiState.Idle)
    val createScheduleState: StateFlow<UiState<ScheduleResponseDto>> = _createScheduleState.asStateFlow()

    private val _updateScheduleState = MutableStateFlow<UiState<ScheduleResponseDto>>(UiState.Idle)
    val updateScheduleState: StateFlow<UiState<ScheduleResponseDto>> = _updateScheduleState.asStateFlow()

    private val _deleteScheduleState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteScheduleState: StateFlow<UiState<Unit>> = _deleteScheduleState.asStateFlow()

    private val _progressState = MutableStateFlow<UiState<ProgressResponseDto>>(UiState.Idle)
    val progressState: StateFlow<UiState<ProgressResponseDto>> = _progressState.asStateFlow()

    /**
     * Fetch schedules for a specific date
     */
    fun fetchSchedules(date: String?) {
        viewModelScope.launch {
            _schedulesState.value = UiState.Loading
            
            repository.getSchedules(date)
                .onSuccess { schedules ->
                    _schedulesState.value = UiState.Success(schedules)
                }
                .onFailure { exception ->
                    _schedulesState.value = UiState.Error(exception.message ?: "Failed to fetch schedules")
                }
        }
    }

    /**
     * Fetch a specific schedule by ID
     */
    fun fetchScheduleById(id: Int) {
        viewModelScope.launch {
            _scheduleDetailState.value = UiState.Loading
            
            repository.getScheduleById(id)
                .onSuccess { schedule ->
                    _scheduleDetailState.value = UiState.Success(schedule)
                }
                .onFailure { exception ->
                    _scheduleDetailState.value = UiState.Error(exception.message ?: "Failed to fetch schedule")
                }
        }
    }

    /**
     * Create a custom schedule
     */
    fun createCustomSchedule(request: CreateCustomScheduleDto) {
        viewModelScope.launch {
            _createScheduleState.value = UiState.Loading
            
            repository.createCustomSchedule(request)
                .onSuccess { schedule ->
                    _createScheduleState.value = UiState.Success(schedule)
                }
                .onFailure { exception ->
                    _createScheduleState.value = UiState.Error(exception.message ?: "Failed to create schedule")
                }
        }
    }

    /**
     * Create recurring schedules
     */
    fun createRecurringSchedule(request: CreateRecurringScheduleDto) {
        viewModelScope.launch {
            _createScheduleState.value = UiState.Loading
            
            repository.createRecurringSchedule(request)
                .onSuccess { schedules ->
                    // Return the first schedule as representative
                    _createScheduleState.value = if (schedules.isNotEmpty()) {
                        UiState.Success(schedules.first())
                    } else {
                        UiState.Error("No schedules created")
                    }
                }
                .onFailure { exception ->
                    _createScheduleState.value = UiState.Error(exception.message ?: "Failed to create recurring schedules")
                }
        }
    }

    /**
     * Update a schedule
     */
    fun updateSchedule(id: Int, request: UpdateScheduleDto) {
        viewModelScope.launch {
            _updateScheduleState.value = UiState.Loading
            
            repository.updateSchedule(id, request)
                .onSuccess { schedule ->
                    _updateScheduleState.value = UiState.Success(schedule)
                }
                .onFailure { exception ->
                    _updateScheduleState.value = UiState.Error(exception.message ?: "Failed to update schedule")
                }
        }
    }

    /**
     * Delete a schedule
     */
    fun deleteSchedule(id: Int) {
        viewModelScope.launch {
            _deleteScheduleState.value = UiState.Loading
            
            repository.deleteSchedule(id)
                .onSuccess {
                    _deleteScheduleState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    _deleteScheduleState.value = UiState.Error(exception.message ?: "Failed to delete schedule")
                }
        }
    }

    /**
     * Create progress for a schedule
     */
    fun createProgress(request: CreateProgressDto) {
        viewModelScope.launch {
            _progressState.value = UiState.Loading
            
            repository.createProgress(request)
                .onSuccess { progress ->
                    _progressState.value = UiState.Success(progress)
                }
                .onFailure { exception ->
                    _progressState.value = UiState.Error(exception.message ?: "Failed to create progress")
                }
        }
    }

    /**
     * Update schedule and then create progress
     */
    fun updateAndCreateProgress(scheduleId: Int, updateRequest: UpdateScheduleDto, progressRequest: CreateProgressDto) {
        viewModelScope.launch {
            _updateScheduleState.value = UiState.Loading
            
            repository.updateSchedule(scheduleId, updateRequest)
                .onSuccess { schedule ->
                    _updateScheduleState.value = UiState.Success(schedule)
                    // Now Create Progress
                    createProgress(progressRequest)
                }
                .onFailure { exception ->
                    _updateScheduleState.value = UiState.Error(exception.message ?: "Failed to update schedule")
                }
        }
    }

    /**
     * Reset states to idle
     */
    fun resetCreateState() {
        _createScheduleState.value = UiState.Idle
    }

    fun resetUpdateState() {
        _updateScheduleState.value = UiState.Idle
    }

    fun resetDeleteState() {
        _deleteScheduleState.value = UiState.Idle
    }

    fun resetProgressState() {
        _progressState.value = UiState.Idle
    }
}
