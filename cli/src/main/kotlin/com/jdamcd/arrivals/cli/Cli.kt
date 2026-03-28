package com.jdamcd.arrivals.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.GtfsSearch
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.SettingsConfig
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.StopSearch
import com.jdamcd.arrivals.TflSearch
import com.jdamcd.arrivals.initKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

fun main(args: Array<String>) {
    initKoin()
    runBlocking {
        Cli()
            .subcommands(
                Tfl(),
                Gtfs(),
                Darwin(),
                Bvg(),
                Search().subcommands(
                    SearchTfl(),
                    SearchStops("darwin", "darwin"),
                    SearchStops("bvg", "bvg") { "${it.name} (${it.id})" }
                ),
                ListStops()
            )
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
        station?.let { settings.stopId = it }
        platform?.let { settings.platform = it }
        direction?.let { settings.direction = it }

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
        .help("Station CRS code (e.g. PMR)")

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
        line?.let { settings.line = it }
        platform?.let { settings.platform = it }

        fetchAndDisplay(arrivals)
    }
}

private class Search : SuspendingCliktCommand("search") {
    override suspend fun run() = Unit
}

private class SearchTfl :
    SuspendingCliktCommand("tfl"),
    KoinComponent {
    private val tflSearch: TflSearch by inject()

    private val query by argument("query")

    override suspend fun run() {
        try {
            val results = tflSearch.searchStops(query)
            if (results.isEmpty()) {
                echo("No results found")
                return
            }
            for (result in results) {
                if (result.isHub) {
                    val details = tflSearch.stopDetails(result.id)
                    echo(yellow("${details.name}:"))
                    for (child in details.children) {
                        echo(yellow("  ${child.name} (${child.id})"))
                    }
                } else {
                    echo(yellow("${result.name} (${result.id})"))
                }
            }
        } catch (e: Exception) {
            echo(e.message)
        }
    }
}

// Search non-hierarchical transit APIs (Darwin, BVG)
private class SearchStops(
    name: String,
    private val qualifier: String,
    private val format: (StopResult) -> String = { it.name }
) : SuspendingCliktCommand(name),
    KoinComponent {
    private val search: StopSearch by inject(named(qualifier))

    private val query by argument("query")

    override suspend fun run() {
        try {
            displayResults(search.searchStops(query), format)
        } catch (e: Exception) {
            echo(e.message)
        }
    }
}

// List active stops in a GTFS realtime feed
private class ListStops :
    SuspendingCliktCommand("list-stops"),
    KoinComponent {
    private val settings: Settings by inject()
    private val gtfsSearch: GtfsSearch by inject()

    private val realtime by option("--realtime")
        .help("GTFS-RT feed URL")
        .required()

    private val schedule by option("--schedule")
        .help("GTFS schedule URL")
        .required()

    private val apiKey by option("--api-key")
        .help("API key for authenticated feeds")

    private val apiKeyParam by option("--api-key-param")
        .help("API key type (e.g. 'app_id', 'header:Authorization')")

    override suspend fun run() {
        settings.gtfsSchedule = schedule
        apiKey?.let { settings.gtfsApiKey = it }
        apiKeyParam?.let { settings.gtfsApiKeyParam = it }
        try {
            displayResults(gtfsSearch.getStops(realtime))
        } catch (e: Exception) {
            echo(e.message)
        }
    }
}

private fun SuspendingCliktCommand.displayResults(
    results: List<StopResult>,
    format: (StopResult) -> String = { it.name }
) {
    if (results.isEmpty()) {
        echo("No results found")
        return
    }
    for (result in results) {
        echo(yellow(format(result)))
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
