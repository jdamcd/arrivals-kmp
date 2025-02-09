package com.jdamcd.arrivals.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.initKoin
import kotlin.system.exitProcess

private val koin = initKoin().koin

fun main(args: Array<String>) = application {
    val fixWindow = args.contains("-pi")
    val width = dimenFromArg(args, 1) ?: 1280
    val height = dimenFromArg(args, 2) ?: 400

    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(width.dp, height.dp)
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

private fun dimenFromArg(args: Array<String>, index: Int): Int? = if (args.size > index) args[index].toIntOrNull() else null
