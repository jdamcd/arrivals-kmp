package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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

internal class GtfsApi(private val client: HttpClient) {

    private val baseDir = getFilesDir()
    private val defaultDir = "$baseDir/gtfs".toPath()
    private val stopsFileName = "stops.txt"

    suspend fun fetchFeedMessage(url: String, apiKey: String = ""): FeedMessage {
        val bodyBytes = client.get(url) {
            if (apiKey.isNotEmpty()) parameter("api_key", apiKey)
        }.bodyAsBytes()
        return FeedMessage.ADAPTER.decode(bodyBytes)
    }

    suspend fun downloadStops(url: String, folder: String = "gtfs", apiKey: String = ""): String {
        val tempZipFile = "$baseDir/gtfs.zip".toPath()
        val outputDir = "$baseDir/$folder".toPath()
        try {
            val zipContent = client.get(url) {
                if (apiKey.isNotEmpty()) parameter("api_key", apiKey)
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

private fun Path.createParentDirectories() {
    this.parent?.let { parent ->
        FileSystem.SYSTEM.createDirectories(parent)
    }
}

expect fun getFilesDir(): String
