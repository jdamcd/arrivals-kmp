package com.jdamcd.arrivals.web

import com.jdamcd.arrivals.NoDataException
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

private const val REFRESH_INTERVAL_MS = 60_000L
private const val DEFAULT_WIDTH = 640.0
private const val DEFAULT_HEIGHT = 200.0

fun main() {
    val target = document.getElementById("arrivals-board") as? HTMLElement ?: return

    val stopId = target.getAttribute("data-stop") ?: return
    val feedKey = target.getAttribute("data-feed") ?: return
    val widthAttr = target.getAttribute("data-width")?.toDoubleOrNull() ?: DEFAULT_WIDTH
    val heightAttr = target.getAttribute("data-height")?.toDoubleOrNull() ?: DEFAULT_HEIGHT

    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.style.display = "block"
    target.appendChild(canvas)

    val renderer = CanvasRenderer(canvas, widthAttr, heightAttr)
    renderer.showLoading()

    val arrivals = MtaArrivals(stopId, feedKey)

    renderer.loadFont().then {
        startRefreshLoop(arrivals, renderer)
    }.catch {
        console.warn("Custom font failed to load, using fallback")
        startRefreshLoop(arrivals, renderer)
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun startRefreshLoop(arrivals: MtaArrivals, renderer: CanvasRenderer) {
    GlobalScope.launch {
        while (true) {
            try {
                val info = arrivals.fetch()
                renderer.update(info)
            } catch (e: NoDataException) {
                renderer.showError(e.message ?: "No data")
            } catch (_: Exception) {
                renderer.showError("No connection")
            }
            delay(REFRESH_INTERVAL_MS)
        }
    }
}
