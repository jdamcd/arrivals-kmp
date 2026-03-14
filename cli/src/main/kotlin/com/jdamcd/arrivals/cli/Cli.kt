package com.jdamcd.arrivals.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.SettingsConfig
import com.jdamcd.arrivals.initKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun main(args: Array<String>) {
    initKoin()
    runBlocking {
        Cli()
            .subcommands(Tfl(), Gtfs(), Darwin(), Bvg())
            .main(args)
    }
}

private class Cli : SuspendingCliktCommand("arrivals") {
    override suspend fun run() = Unit
}

private class Tfl :
    SuspendingCliktCommand("tfl"),
    KoinComponent {
    private val arrivals: Arrivals by inject()
    private val settings: Settings by inject()

    private val station by option("--station")
        .help("Stop ID (e.g. 910GSHRDHST)")

    private val platform by option("--platform")
        .help("Platform filter (optional)")

    private val direction by option("--direction")
        .choice("inbound", "outbound", "all")
        .help("Direction filter (optional)")

    override suspend fun run() {
        settings.mode = SettingsConfig.MODE_TFL
        station?.let { settings.stopId = it }
        platform?.let { settings.platform = it }
        direction?.let { settings.tflDirection = it }

        fetchAndDisplay(arrivals)
    }
}

private class Gtfs :
    SuspendingCliktCommand("gtfs"),
    KoinComponent {
    private val arrivals: Arrivals by inject()
    private val settings: Settings by inject()

    private val stop by option("--station")
        .help("Stop ID (e.g. A42N)")

    private val realtime by option("--realtime")
        .help("GTFS-RT feed URL")

    private val schedule by option("--schedule")
        .help("GTFS schedule URL")

    private val apiKeyParam by option("--api-key-param")
        .help("API key type (e.g. 'app_id', 'header:Authorization')")

    private val apiKey by option("--api-key")
        .help("API key for authenticated feeds")

    override suspend fun run() {
        settings.mode = SettingsConfig.MODE_GTFS
        stop?.let { settings.stopId = it }
        realtime?.let { settings.gtfsRealtime = it }
        schedule?.let { settings.gtfsSchedule = it }
        apiKey?.let { settings.gtfsApiKey = it }
        apiKeyParam?.let { settings.gtfsApiKeyParam = it }

        fetchAndDisplay(arrivals)
    }
}

private class Darwin :
    SuspendingCliktCommand("darwin"),
    KoinComponent {
    private val arrivals: Arrivals by inject()
    private val settings: Settings by inject()

    private val station by option("--station")
        .help("Stop ID / CRS code (e.g. PMR)")

    private val platform by option("--platform")
        .help("Platform filter (optional)")

    override suspend fun run() {
        settings.mode = SettingsConfig.MODE_DARWIN
        station?.let { settings.stopId = it }
        platform?.let { settings.platform = it }

        fetchAndDisplay(arrivals)
    }
}

private class Bvg :
    SuspendingCliktCommand("bvg"),
    KoinComponent {
    private val arrivals: Arrivals by inject()
    private val settings: Settings by inject()

    private val station by option("--station")
        .help("Stop ID (e.g. 900013102)")

    private val line by option("--line")
        .help("Line filter (e.g. U2, S5, M10)")

    private val platform by option("--platform")
        .help("Platform filter (optional)")

    override suspend fun run() {
        settings.mode = SettingsConfig.MODE_BVG
        station?.let { settings.stopId = it }
        line?.let { settings.bvgLine = it }
        platform?.let { settings.platform = it }

        fetchAndDisplay(arrivals)
    }
}

private suspend fun SuspendingCliktCommand.fetchAndDisplay(arrivals: Arrivals) {
    try {
        val result = arrivals.latest()
        echo(yellow(result.station))
        echo(
            table {
                tableBorders = Borders.ALL
                cellBorders = Borders.NONE
                column(0) { width = ColumnWidth.Fixed(24) }
                column(1) { align = TextAlign.RIGHT }
                body {
                    result.arrivals.forEach {
                        row(yellow(it.destination.take(24)), yellow(it.time))
                    }
                }
            }
        )
    } catch (e: Exception) {
        echo(e.message)
    }
}
