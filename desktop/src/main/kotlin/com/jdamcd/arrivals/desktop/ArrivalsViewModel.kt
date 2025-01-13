package com.jdamcd.arrivals.desktop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArrivalsViewModel(arrivals: Arrivals) : ViewModel() {

    private val _uiState = MutableStateFlow<ArrivalsState>(ArrivalsState.Loading)
    val uiState: StateFlow<ArrivalsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                try {
                    val result = arrivals.latest()
                    _uiState.value = ArrivalsState.Data(result)
                } catch (e: Exception) {
                    if (_uiState.value !is ArrivalsState.Data) {
                        _uiState.value = ArrivalsState.Error(e.message ?: "Unknown error")
                    }
                }
                delay(60_000L) // 60 seconds
            }
        }
    }
}

sealed class ArrivalsState {
    data object Loading : ArrivalsState()
    data class Data(val result: ArrivalsInfo) : ArrivalsState()
    data class Error(val message: String) : ArrivalsState()
}
