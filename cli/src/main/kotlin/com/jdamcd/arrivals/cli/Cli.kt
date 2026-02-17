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
            .subcommands(Tfl(), Gtfs(), Darwin())
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
        .help("Station ID (e.g. 910GSHRDHST)")

    private val platform by option("--platform")
        .help("Platform filter (optional)")

    private val direction by option("--direction")
        .choice("inbound", "outbound", "all")
        .help("Direction filter (optional)")

    override suspend fun run() {
        settings.mode = SettingsConfig.MODE_TFL
        station?.let { settings.tflStopId = it }
        platform?.let { settings.tflPlatform = it }
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
        .help("Station stop ID (e.g. A42N)")

    private val realtime by option("--realtime")
        .help("GTFS-RT feed URL")

    private val schedule by option("--schedule")
        .help("GTFS schedule URL")

    override suspend fun run() {
        settings.mode = SettingsConfig.MODE_GTFS
        stop?.let { settings.gtfsStop = it }
        realtime?.let { settings.gtfsRealtime = it }
        schedule?.let { settings.gtfsSchedule = it }

        fetchAndDisplay(arrivals)
    }
}

private class Darwin :
    SuspendingCliktCommand("darwin"),
    KoinComponent {
    private val arrivals: Arrivals by inject()
    private val settings: Settings by inject()

    private val station by option("--station")
        .help("Station CRS code (e.g. PMR)")

    private val platform by option("--platform")
        .help("Platform filter (optional)")

    override suspend fun run() {
        settings.mode = SettingsConfig.MODE_DARWIN
        station?.let { settings.darwinCrsCode = it }
        platform?.let { settings.darwinPlatform = it }

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
