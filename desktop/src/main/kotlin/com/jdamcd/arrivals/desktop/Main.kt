package com.jdamcd.arrivals.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.SettingsConfig
import com.jdamcd.arrivals.initKoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import kotlin.system.exitProcess

private val koin = initKoin().koin
private val configFile = File("${System.getProperty("user.home")}/.arrivals.yml")

fun main(args: Array<String>) = application {
    val fixWindow = args.contains("-pi")
    val width = dimenFromArg(args, 1) ?: 1280
    val height = dimenFromArg(args, 2) ?: 400

    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(width.dp, height.dp)
    )

    val settings = remember { koin.get<Settings>() }
    val viewModel = remember {
        loadConfig(settings)
        ArrivalsViewModel(koin.get<Arrivals>())
    }
    val state: ArrivalsState by viewModel.uiState.collectAsState(ArrivalsState.Loading)

    watchConfig(settings, viewModel::refresh)

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

@Composable
private fun watchConfig(settings: Settings, onReload: () -> Unit) {
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val watchService = FileSystems.getDefault().newWatchService()
        val job = scope.launch(Dispatchers.IO) {
            val dir = configFile.parentFile.toPath()
            dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
            val fileName = configFile.name

            while (true) {
                val key = watchService.take()
                for (event in key.pollEvents()) {
                    val changed = event.context() as? Path
                    if (changed?.toString() == fileName) {
                        loadConfig(settings)
                        onReload()
                    }
                }
                key.reset()
            }
        }

        onDispose {
            job.cancel()
            watchService.close()
        }
    }
}

fun loadConfig(settings: Settings) {
    if (configFile.exists()) {
        try {
            FileInputStream(configFile).use { inputStream ->
                val data: Map<String, Any> = Yaml().load(inputStream)
                data.getString(SettingsConfig.MODE)?.let { settings.mode = it }
                data.getString(SettingsConfig.GTFS_REALTIME)?.let { settings.gtfsRealtime = it }
                data.getString(SettingsConfig.GTFS_SCHEDULE)?.let { settings.gtfsSchedule = it }
                data.getString(SettingsConfig.GTFS_STOP)?.let { settings.gtfsStop = it }
                data.getString(SettingsConfig.GTFS_API_KEY)?.let { settings.gtfsApiKey = it }
                data.getString(SettingsConfig.GTFS_API_KEY_PARAM)?.let { settings.gtfsApiKeyParam = it }
                data.getString(SettingsConfig.TFL_STOP)?.let { settings.tflStopId = it }
                data.getString(SettingsConfig.TFL_PLATFORM)?.let { settings.tflPlatform = it }
                data.getString(SettingsConfig.TFL_DIRECTION)?.let { settings.tflDirection = it }
                data.getString(SettingsConfig.DARWIN_CRS)?.let { settings.darwinCrsCode = it }
                data.getString(SettingsConfig.DARWIN_PLATFORM)?.let { settings.darwinPlatform = it }
            }
        } catch (e: Exception) {
            println("Error reading config file: ${e.message}")
        }
    }
}

private fun Map<String, Any>.getString(key: String): String? = get(key)?.toString()
