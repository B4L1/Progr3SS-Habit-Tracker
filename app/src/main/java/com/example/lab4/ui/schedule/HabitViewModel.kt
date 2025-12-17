package com.example.lab4.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab4.data.model.CreateHabitDto
import com.example.lab4.data.model.HabitCategoryResponseDto
import com.example.lab4.data.model.HabitResponseDto
import com.example.lab4.data.repository.HabitRepository
import com.example.lab4.data.repository.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for habit operations
 * Used by CreateHabitFragment and CreateHabitDialogFragment
 */
class HabitViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _habitsState = MutableStateFlow<UiState<List<HabitResponseDto>>>(UiState.Idle)
    val habitsState: StateFlow<UiState<List<HabitResponseDto>>> = _habitsState.asStateFlow()

    private val _createHabitState = MutableStateFlow<UiState<HabitResponseDto>>(UiState.Idle)
    val createHabitState: StateFlow<UiState<HabitResponseDto>> = _createHabitState.asStateFlow()

    private val _categoriesState = MutableStateFlow<UiState<List<HabitCategoryResponseDto>>>(UiState.Idle)
    val categoriesState: StateFlow<UiState<List<HabitCategoryResponseDto>>> = _categoriesState.asStateFlow()

    /**
     * Fetch all habits
     */
    fun fetchHabits() {
        viewModelScope.launch {
            _habitsState.value = UiState.Loading
            
            repository.getHabits()
                .onSuccess { habits ->
                    _habitsState.value = UiState.Success(habits)
                }
                .onFailure { exception ->
                    _habitsState.value = UiState.Error(exception.message ?: "Failed to fetch habits")
                }
        }
    }

    /**
     * Create a new habit
     */
    fun createHabit(request: CreateHabitDto) {
        viewModelScope.launch {
            _createHabitState.value = UiState.Loading
            
            repository.createHabit(request)
                .onSuccess { habit ->
                    _createHabitState.value = UiState.Success(habit)
                }
                .onFailure { exception ->
                    _createHabitState.value = UiState.Error(exception.message ?: "Failed to create habit")
                }
        }
    }

    /**
     * Fetch habit categories
     */
    fun fetchCategories() {
        viewModelScope.launch {
            _categoriesState.value = UiState.Loading
            
            repository.getHabitCategories()
                .onSuccess { categories ->
                    _categoriesState.value = UiState.Success(categories)
                }
                .onFailure { exception ->
                    _categoriesState.value = UiState.Error(exception.message ?: "Failed to fetch categories")
                }
        }
    }

    /**
     * Reset states to idle
     */
    fun resetCreateState() {
        _createHabitState.value = UiState.Idle
    }
}
