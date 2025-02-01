package com.jdamcd.arrivals.desktop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.NoDataException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArrivalsViewModel(private val arrivals: Arrivals) : ViewModel() {

    private val _uiState = MutableStateFlow<ArrivalsState>(ArrivalsState.Loading)
    val uiState: StateFlow<ArrivalsState> = _uiState.asStateFlow()

    private var job: Job? = null

    init {
        startUpdates()
    }

    fun refresh() {
        startUpdates()
    }

    private fun startUpdates() {
        job?.cancel()
        job = viewModelScope.launch {
            while (true) {
                update()
                delay(60_000L) // 60 seconds
            }
        }
    }

    private suspend fun update() {
        if (_uiState.value is ArrivalsState.Data) {
            _uiState.value = ArrivalsState.Data((_uiState.value as ArrivalsState.Data).result, true)
        }
        try {
            val result = arrivals.latest()
            _uiState.value = ArrivalsState.Data(result, false)
        } catch (e: NoDataException) {
            _uiState.value = ArrivalsState.Error(e.message ?: "Unknown error")
        } catch (e: Exception) {
            if (_uiState.value !is ArrivalsState.Data) {
                _uiState.value = ArrivalsState.Error("Unknown error")
            }
        }
    }
}

sealed class ArrivalsState {
    data object Loading : ArrivalsState()
    data class Data(val result: ArrivalsInfo, val refreshing: Boolean) : ArrivalsState()
    data class Error(val message: String) : ArrivalsState()
}
