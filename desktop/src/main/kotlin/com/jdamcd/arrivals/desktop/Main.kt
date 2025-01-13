package com.jdamcd.arrivals.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.initKoin

private val koin = initKoin().koin

fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(1280.dp, 400.dp)
    )

    val viewModel = ArrivalsViewModel(koin.get<Arrivals>())
    val state: ArrivalsState by viewModel.uiState.collectAsState(ArrivalsState.Loading)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Arrivals"
    ) {
        ArrivalsList(state)
    }
}
