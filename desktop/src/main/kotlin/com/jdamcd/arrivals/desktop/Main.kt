package com.jdamcd.arrivals.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.initKoin
import kotlin.system.exitProcess

private val koin = initKoin().koin

fun main(args: Array<String>) = application {
    val fixWindow = args.contains("-pi")

    val windowState = rememberWindowState(
        placement = if (fixWindow) WindowPlacement.Maximized else WindowPlacement.Floating,
        size = DpSize(1280.dp, 400.dp)
    )

    val viewModel = ArrivalsViewModel(koin.get<Arrivals>())
    val state: ArrivalsState by viewModel.uiState.collectAsState(ArrivalsState.Loading)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Arrivals",
        undecorated = fixWindow,
        resizable = !fixWindow,
        onKeyEvent = {
            if (it.key == Key.Escape) {
                exitProcess(0)
            }
            false
        }
    ) {
        ArrivalsView(state, viewModel::refresh)
    }
}
