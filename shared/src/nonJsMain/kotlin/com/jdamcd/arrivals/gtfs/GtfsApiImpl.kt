package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.readRawBytes
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use

internal actual fun createGtfsApi(client: HttpClient): GtfsApi = GtfsApiImpl(client)

internal class GtfsApiImpl(private val client: HttpClient) : GtfsApi {

    private val baseDir = getFilesDir()
    private val stopsFileName = "stops.txt"
    private val routesFileName = "routes.txt"
    private val sourceFileName = "stops.source"

    override suspend fun fetchFeedMessage(url: String, auth: ApiAuth?): FeedMessage {
        val bodyBytes = client.get(url) {
            auth.applyTo(this)
        }.bodyAsBytes()
        return FeedMessage.ADAPTER.decode(bodyBytes)
    }

    override suspend fun downloadSchedule(url: String, folder: String, auth: ApiAuth?) {
        val tempZipFile = "$baseDir/gtfs.zip".toPath()
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
            unpackZip(tempZipFile, dirPath(folder))
            writeStopsSource(url, folder)
        } finally {
            FileSystem.SYSTEM.delete(tempZipFile)
        }
    }

    private fun dirPath(folder: String = "gtfs") = "$baseDir/$folder".toPath()

    override fun hasStops(): Boolean = FileSystem.SYSTEM.exists(dirPath().resolve(stopsFileName))

    override fun stopsLastModifiedEpochSeconds(): Long {
        val metadata = FileSystem.SYSTEM.metadata(dirPath().resolve(stopsFileName))
        return metadata.lastModifiedAtMillis?.div(1000) ?: 0L
    }

    override fun stopsSource(): String? = try {
        FileSystem.SYSTEM.read(dirPath().resolve(sourceFileName)) { readUtf8() }.trim()
    } catch (_: Exception) {
        null
    }

    private fun writeStopsSource(url: String, folder: String = "gtfs") {
        FileSystem.SYSTEM.write(dirPath(folder).resolve(sourceFileName)) { writeUtf8(url) }
    }

    override fun readStops(folder: String): String = FileSystem.SYSTEM.read(dirPath(folder).resolve(stopsFileName)) { readUtf8() }

    override fun readRoutes(folder: String): String? {
        val routesPath = dirPath(folder).resolve(routesFileName)
        return if (FileSystem.SYSTEM.exists(routesPath)) {
            FileSystem.SYSTEM.read(routesPath) { readUtf8() }
        } else {
            null // Optional file in GTFS spec
        }
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

private fun Path.createParentDirectories() {
    this.parent?.let { parent ->
        FileSystem.SYSTEM.createDirectories(parent)
    }
}
