package com.jdamcd.arrivals

import com.jdamcd.arrivals.bvg.BvgApi
import com.jdamcd.arrivals.bvg.BvgArrivals
import com.jdamcd.arrivals.darwin.DarwinApi
import com.jdamcd.arrivals.darwin.DarwinArrivals
import com.jdamcd.arrivals.gtfs.ApiAuth
import com.jdamcd.arrivals.gtfs.GtfsApi
import com.jdamcd.arrivals.gtfs.GtfsArrivals
import com.jdamcd.arrivals.gtfs.GtfsStopSearch
import com.jdamcd.arrivals.gtfs.createGtfsApi
import com.jdamcd.arrivals.gtfs.system.Bart
import com.jdamcd.arrivals.gtfs.system.Mta
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
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.coroutines.cancellation.CancellationException
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
    val mtaSearch: GtfsSearch by inject(named("mta"))
    val bartSearch: GtfsSearch by inject(named("bart"))
    val darwinSearch: StopSearch by inject(named("darwin"))
    val bvgSearch: StopSearch by inject(named("bvg"))
}

@OptIn(kotlin.time.ExperimentalTime::class)
fun commonModule() = module {
    single<Clock> { Clock.System }
    single { Settings() }
    single { TflApi(get()) }
    single<GtfsApi> { createGtfsApi(get()) }
    single { DarwinApi(get()) }
    single { TflArrivals(get(), get(), get()) }
    single { GtfsArrivals(get(), get(), get()) }
    single { DarwinArrivals(get(), get(), get()) }
    single { BvgApi(get()) }
    single { BvgArrivals(get(), get(), get()) }
    single<Arrivals> { ArrivalsSwitcher(get(), get(), get(), get(), get()) }
    single<TflSearch> { get<TflArrivals>() }
    factory<GtfsSearch>(named("mta")) { GtfsStopSearch(get(), Mta.SCHEDULE, "mta") }
    factory<GtfsSearch>(named("bart")) { GtfsStopSearch(get(), Bart.SCHEDULE, "bart", ApiAuth.QueryParam("api_key", Bart.API_KEY)) }
    factory<GtfsSearch> {
        val settings: Settings = get()
        GtfsStopSearch(get(), settings.gtfsSchedule, "gtfs", ApiAuth.parse(settings.gtfsApiKey, settings.gtfsApiKeyParam))
    }
    single<StopSearch>(named("darwin")) { get<DarwinArrivals>() }
    single<StopSearch>(named("bvg")) { get<BvgArrivals>() }
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
    private val bvg: BvgArrivals,
    private val settings: Settings
) : Arrivals {

    override suspend fun latest(): ArrivalsInfo = when (settings.mode) {
        SettingsConfig.MODE_TFL -> tfl.latest()
        SettingsConfig.MODE_DARWIN -> darwin.latest()
        SettingsConfig.MODE_BVG -> bvg.latest()
        else -> gtfs.latest()
    }
}

interface Arrivals {
    @Throws(NoDataException::class, CancellationException::class)
    suspend fun latest(): ArrivalsInfo
}

interface TflSearch {
    @Throws(Exception::class, CancellationException::class)
    suspend fun searchStops(query: String): List<StopResult>

    @Throws(Exception::class, CancellationException::class)
    suspend fun stopDetails(id: String): StopDetails
}

interface GtfsSearch {
    @Throws(Exception::class, CancellationException::class)
    suspend fun getStops(feedUrl: String): List<StopResult>
}

interface StopSearch {
    @Throws(Exception::class, CancellationException::class)
    suspend fun searchStops(query: String): List<StopResult>
}

data class ArrivalsInfo(
    val station: String,
    val arrivals: List<Arrival>
)

data class Arrival(
    val id: Int,
    val destination: String,
    val secondsToStop: Int,
    val realtime: Boolean = true,
    val line: String? = null,
    val lineBadge: LineBadge? = null
) {
    val displayName: String
        get() = listOfNotNull(line, destination).joinToString(" - ")

    val displayTime: String
        get() = formatTime(secondsToStop, realtime)

    val minutesToStop: Int
        get() = maxOf(0, secondsToStop / 60)

    val isDue: Boolean
        get() = secondsToStop < 60
}

data class LineBadge(
    val label: String,
    val color: String,
    val textColor: String?,
    val express: Boolean = false
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
