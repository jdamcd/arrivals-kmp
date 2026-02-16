package com.jdamcd.arrivals

import com.jdamcd.arrivals.darwin.DarwinApi
import com.jdamcd.arrivals.darwin.DarwinArrivals
import com.jdamcd.arrivals.gtfs.GtfsApi
import com.jdamcd.arrivals.gtfs.GtfsArrivals
import com.jdamcd.arrivals.gtfs.MtaSearch
import com.jdamcd.arrivals.tfl.TflApi
import com.jdamcd.arrivals.tfl.TflArrivals
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Clock

@Suppress("Unused")
fun initKoin() = startKoin {
    modules(commonModule())
}

@Suppress("Unused")
class MacDI : KoinComponent {
    val arrivals: Arrivals by inject()
    val settings: Settings by inject()
    val tflSearch: TflSearch by inject()
    val gtfsSearch: GtfsSearch by inject()
    val darwinSearch: DarwinSearch by inject()
}

@OptIn(kotlin.time.ExperimentalTime::class)
fun commonModule() = module {
    single<Clock> { Clock.System }
    single { Settings() }
    single { TflApi(get()) }
    single { GtfsApi(get()) }
    single { DarwinApi(get()) }
    single { TflArrivals(get(), get()) }
    single { GtfsArrivals(get(), get(), get()) }
    single { DarwinArrivals(get(), get(), get()) }
    single<Arrivals> { ArrivalsSwitcher(get(), get(), get(), get()) }
    single<TflSearch> { get<TflArrivals>() }
    single<GtfsSearch> { MtaSearch(get()) }
    single<DarwinSearch> { get<DarwinArrivals>() }
    single {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000 // 10 seconds
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
}

internal class ArrivalsSwitcher(
    private val tfl: TflArrivals,
    private val gtfs: GtfsArrivals,
    private val darwin: DarwinArrivals,
    private val settings: Settings
) : Arrivals {

    override suspend fun latest(): ArrivalsInfo = when (settings.mode) {
        SettingsConfig.MODE_TFL -> tfl.latest()
        SettingsConfig.MODE_DARWIN -> darwin.latest()
        else -> gtfs.latest()
    }
}

interface Arrivals {
    @Throws(NoDataException::class, CancellationException::class)
    suspend fun latest(): ArrivalsInfo
}

interface TflSearch {
    @Throws(CancellationException::class)
    suspend fun searchStops(query: String): List<StopResult>

    @Throws(CancellationException::class)
    suspend fun stopDetails(id: String): StopDetails
}

interface GtfsSearch {
    @Throws(CancellationException::class)
    suspend fun getStops(feedUrl: String): List<StopResult>
}

interface DarwinSearch {
    @Throws(CancellationException::class)
    suspend fun searchStops(query: String): List<StopResult>
}

data class ArrivalsInfo(
    val station: String,
    val arrivals: List<Arrival>
)

data class Arrival(
    val id: Int,
    val destination: String,
    val time: String,
    val secondsToStop: Int
)

data class StopResult(
    val id: String,
    val name: String,
    val isHub: Boolean
)

data class StopDetails(
    val id: String,
    val name: String,
    val children: List<StopResult>
)

class NoDataException(
    message: String
) : Exception(message)

fun formatTime(seconds: Int) = if (seconds < 60) {
    "Due"
} else {
    "${(seconds / 60f).roundToInt()} min"
}

fun sanitizePlatform(input: String): String = if (input.startsWith("Platform ", ignoreCase = true)) {
    input.substring(9).trim()
} else {
    input.trim()
}
