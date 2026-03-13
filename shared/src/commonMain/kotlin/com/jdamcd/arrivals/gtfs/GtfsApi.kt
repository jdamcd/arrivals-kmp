package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
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

internal class GtfsApi(private val client: HttpClient) {

    private val baseDir = getFilesDir()
    private val defaultDir = "$baseDir/gtfs".toPath()
    private val stopsFileName = "stops.txt"

    suspend fun fetchFeedMessage(url: String, auth: ApiAuth? = null): FeedMessage {
        val bodyBytes = client.get(url) {
            auth.applyTo(this)
        }.bodyAsBytes()
        return FeedMessage.ADAPTER.decode(bodyBytes)
    }

    suspend fun downloadStops(url: String, folder: String = "gtfs", auth: ApiAuth? = null): String {
        val tempZipFile = "$baseDir/gtfs.zip".toPath()
        val outputDir = "$baseDir/$folder".toPath()
        try {
            val zipContent = client.get(url) {
                auth.applyTo(this)
                timeout {
                    requestTimeoutMillis = 60_000 // 1 min for large schedule zips
                }
            }.readRawBytes()
            FileSystem.SYSTEM.write(tempZipFile) {
                write(zipContent)
            }
            unpackZip(tempZipFile, outputDir)
            return readStops(outputDir)
        } finally {
            FileSystem.SYSTEM.delete(tempZipFile)
        }
    }

    fun hasStops(): Boolean = FileSystem.SYSTEM.exists(defaultDir.resolve(stopsFileName))

    fun readStops(dir: Path = defaultDir): String {
        val stopsPath = dir.resolve(stopsFileName)
        return FileSystem.SYSTEM.read(stopsPath) { readUtf8() }
    }

    private fun unpackZip(source: Path, destination: Path) {
        val zipFile = FileSystem.SYSTEM.openZip(source)
        val paths = zipFile.listRecursively("/".toPath())
            .filter { zipFile.metadata(it).isRegularFile }
            .toList()

        paths.forEach { filePath ->
            zipFile.source(filePath).buffer().use { source ->
                val relativePath = filePath.toString().trimStart('/')
                val fileToWrite = destination.resolve(relativePath)
                fileToWrite.createParentDirectories()
                FileSystem.SYSTEM.sink(fileToWrite).buffer().use { sink ->
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

private fun Path.createParentDirectories() {
    this.parent?.let { parent ->
        FileSystem.SYSTEM.createDirectories(parent)
    }
}

expect fun getFilesDir(): String
