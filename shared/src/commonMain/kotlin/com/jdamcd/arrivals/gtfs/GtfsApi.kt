package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import com.jdamcd.arrivals.HttpApiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.readRawBytes
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use

internal sealed class ApiAuth {
    data class QueryParam(val name: String, val key: String) : ApiAuth()
    data class Header(val name: String, val key: String) : ApiAuth()

    companion object Companion {
        fun parse(key: String, param: String = ""): ApiAuth? {
            if (key.isEmpty()) return null
            if (param.isEmpty()) return QueryParam("api_key", key)
            if (param.startsWith("header:")) return Header(param.removePrefix("header:"), key)
            val name = param.removePrefix("query:")
            return QueryParam(name, key)
        }
    }
}

internal class GtfsApi(
    client: HttpClient,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) : HttpApiClient(
    client = client,
    apiName = "GTFS feed"
) {

    private val baseDir = getFilesDir()
    private val stopsFileName = "stops.txt"
    private val routesFileName = "routes.txt"
    private val sourceFileName = "stops.source"

    suspend fun fetchFeedMessage(url: String, auth: ApiAuth? = null): FeedMessage {
        val bodyBytes = executeRequest(url) {
            auth.applyTo(this)
        }.bodyAsBytes()
        return FeedMessage.ADAPTER.decode(bodyBytes)
    }

    suspend fun downloadSchedule(url: String, folder: String = "gtfs", auth: ApiAuth? = null) {
        val tempZipFile = "$baseDir/$folder.zip".toPath()
        try {
            val zipContent = executeRequest(url) {
                auth.applyTo(this)
                timeout {
                    requestTimeoutMillis = 60_000 // 1 min for large schedule zips
                }
            }.readRawBytes()
            fileSystem.write(tempZipFile) {
                write(zipContent)
            }
            unpackZip(tempZipFile, dirPath(folder))
            writeStopsSource(url, folder)
        } finally {
            fileSystem.delete(tempZipFile)
        }
    }

    private fun dirPath(folder: String = "gtfs") = "$baseDir/$folder".toPath()

    fun hasStops(): Boolean = fileSystem.exists(dirPath().resolve(stopsFileName))

    fun stopsLastModifiedEpochSeconds(): Long {
        val metadata = fileSystem.metadata(dirPath().resolve(stopsFileName))
        return metadata.lastModifiedAtMillis?.div(1000) ?: 0L
    }

    fun stopsSource(): String? = try {
        fileSystem.read(dirPath().resolve(sourceFileName)) { readUtf8() }.trim()
    } catch (_: Exception) {
        null
    }

    private fun writeStopsSource(url: String, folder: String = "gtfs") {
        fileSystem.write(dirPath(folder).resolve(sourceFileName)) { writeUtf8(url) }
    }

    fun readStops(folder: String = "gtfs"): String = fileSystem.read(dirPath(folder).resolve(stopsFileName)) { readUtf8() }

    fun readRoutes(folder: String = "gtfs"): String? {
        val routesPath = dirPath(folder).resolve(routesFileName)
        return if (fileSystem.exists(routesPath)) {
            fileSystem.read(routesPath) { readUtf8() }
        } else {
            null // Optional file in GTFS spec
        }
    }

    private fun unpackZip(source: Path, destination: Path) {
        val zipFile = fileSystem.openZip(source)
        val paths = zipFile.listRecursively("/".toPath())
            .filter { zipFile.metadata(it).isRegularFile }
            .toList()

        paths.forEach { filePath ->
            zipFile.source(filePath).buffer().use { source ->
                val relativePath = filePath.toString().trimStart('/')
                val fileToWrite = destination.resolve(relativePath)
                fileToWrite.parent?.let { fileSystem.createDirectories(it) }
                fileSystem.sink(fileToWrite).buffer().use { sink ->
                    sink.writeAll(source)
                }
            }
        }
    }
}

private fun ApiAuth?.applyTo(builder: HttpRequestBuilder) {
    when (this) {
        is ApiAuth.QueryParam -> builder.parameter(name, key)
        is ApiAuth.Header -> builder.header(name, key)
        null -> {}
    }
}

expect fun getFilesDir(): String
