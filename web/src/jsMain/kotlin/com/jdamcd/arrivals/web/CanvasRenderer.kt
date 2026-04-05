package com.jdamcd.arrivals.web

import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.filterLedChars
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.Promise
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

internal class CanvasRenderer(
    private val canvas: HTMLCanvasElement,
    private val width: Double,
    private val height: Double
) {
    private val ctx: dynamic = canvas.getContext("2d")
    private val dpr = window.devicePixelRatio

    private var state: BoardState = BoardState.Loading
    private var animationFrameId: Int? = null

    private val boardHeight = height - FOOTER_HEIGHT
    private val rowHeight = boardHeight / 3.6
    private val dataFontSize = rowHeight * TEXT_SCALE
    private val dataFont = "${dataFontSize}px $FONT_FAMILY"
    private val errorFontSize = boardHeight * TEXT_SCALE / 3
    private val errorFont = "${errorFontSize}px $FONT_FAMILY"
    private val footerFont = "${FOOTER_FONT_SIZE}px sans-serif"
    private val totalRowsHeight = rowHeight * 3
    private val rowOffsetY = (boardHeight - totalRowsHeight) / 2 + 2.0
    private val loadingCenterX = width / 2
    private val loadingCenterY = height / 2
    private val loadingRadius = minOf(loadingCenterX, loadingCenterY) * 0.15

    init {
        canvas.width = (width * dpr).toInt()
        canvas.height = (height * dpr).toInt()
        canvas.style.width = "${width}px"
        canvas.style.height = "${height}px"
        ctx.scale(dpr, dpr)
    }

    fun loadFont(): Promise<Unit> {
        val src = "url(data:font/ttf;base64,$LUR_FONT_BASE64)"
        window.asDynamic()._arrivalsFont = src
        val face: dynamic = js("new FontFace('LUR', window._arrivalsFont)")
        js("delete window._arrivalsFont")
        return (face.load() as Promise<dynamic>).then<Unit> { loaded ->
            document.asDynamic().fonts.add(loaded)
        }
    }

    fun update(info: ArrivalsInfo) {
        state = BoardState.Data(
            station = info.station,
            rows = info.arrivals.map { prepareRow(it) },
            hasAnimation = info.arrivals.any { it.isDue }
        )
        scheduleRender()
    }

    fun showError(message: String) {
        state = BoardState.Error(filterLedChars(message))
        renderOnce()
    }

    fun showLoading() {
        state = BoardState.Loading
        startAnimationLoop()
    }

    private fun scheduleRender() {
        stopAnimationLoop()
        val s = state
        if (s is BoardState.Data && s.hasAnimation) {
            startAnimationLoop()
        } else {
            renderOnce()
        }
    }

    private fun renderOnce() {
        stopAnimationLoop()
        render(0.0)
    }

    private fun startAnimationLoop() {
        if (animationFrameId != null) return
        fun loop(timestamp: Double) {
            render(timestamp)
            animationFrameId = window.requestAnimationFrame(::loop)
        }
        animationFrameId = window.requestAnimationFrame(::loop)
    }

    private fun stopAnimationLoop() {
        animationFrameId?.let { window.cancelAnimationFrame(it) }
        animationFrameId = null
    }

    private fun render(timestamp: Double) {
        ctx.fillStyle = COLOR_BACKGROUND
        ctx.fillRect(0.0, 0.0, width, height)

        when (val s = state) {
            is BoardState.Loading -> renderLoading(timestamp)
            is BoardState.Data -> renderData(s, timestamp)
            is BoardState.Error -> renderError(s.message)
        }
    }

    private fun renderLoading(timestamp: Double) {
        val startAngle = (timestamp / 300.0) % (2 * PI)
        ctx.save()
        ctx.strokeStyle = COLOR_LED_YELLOW
        ctx.lineWidth = loadingRadius * 0.25
        ctx.lineCap = "round"
        ctx.beginPath()
        ctx.arc(loadingCenterX, loadingCenterY, loadingRadius, startAngle, startAngle + PI * 1.5)
        ctx.stroke()
        ctx.restore()
    }

    private fun renderData(data: BoardState.Data, timestamp: Double) {
        ctx.font = dataFont
        ctx.textBaseline = "middle"
        ctx.fillStyle = COLOR_LED_YELLOW

        data.rows.forEachIndexed { index, row ->
            val y = rowOffsetY + rowHeight * index + rowHeight / 2

            if (row.nameWidth > row.maxNameWidth) {
                ctx.save()
                ctx.beginPath()
                ctx.rect(PADDING_H, 0.0, row.maxNameWidth, height)
                ctx.clip()
                ctx.fillText(row.name, PADDING_H, y)
                ctx.restore()
            } else {
                ctx.fillText(row.name, PADDING_H, y)
            }

            if (row.isDue) {
                val cycle = 1500.0
                val alpha = abs(sin(PI * (timestamp % cycle) / cycle))
                ctx.save()
                ctx.globalAlpha = alpha
                ctx.fillText(row.time, width - PADDING_H - row.timeWidth, y)
                ctx.restore()
            } else {
                ctx.fillText(row.time, width - PADDING_H - row.timeWidth, y)
            }
        }

        renderFooter(data.station)
    }

    private fun renderError(message: String) {
        ctx.fillStyle = COLOR_LED_YELLOW
        ctx.font = errorFont
        ctx.textBaseline = "middle"
        ctx.fillText(message, PADDING_H, (height - FOOTER_HEIGHT) / 2)

        renderFooter("")
    }

    private fun renderFooter(station: String) {
        val footerY = height - FOOTER_HEIGHT
        ctx.fillStyle = COLOR_FOOTER
        ctx.fillRect(0.0, footerY, width, FOOTER_HEIGHT)

        ctx.fillStyle = COLOR_TEXT
        ctx.font = footerFont
        ctx.textBaseline = "middle"
        ctx.fillText(station, PADDING_H, footerY + FOOTER_HEIGHT / 2)
    }

    private fun prepareRow(arrival: Arrival): PreparedRow {
        val name = filterLedChars(arrival.displayName)
        val time = filterLedChars(arrival.displayTime)
        ctx.font = dataFont
        val nameWidth = ctx.measureText(name).width as Double
        val timeWidth = ctx.measureText(time).width as Double
        val maxNameWidth = width - PADDING_H * 2 - timeWidth - PADDING_H
        return PreparedRow(name, time, arrival.isDue, nameWidth, timeWidth, maxNameWidth)
    }

    companion object {
        private const val TEXT_SCALE = 0.58
        private const val FOOTER_HEIGHT = 40.0
        private const val FOOTER_FONT_SIZE = 18.0
        private const val PADDING_H = 16.0
        private const val FONT_FAMILY = "LUR, monospace"
        private const val COLOR_BACKGROUND = "#000000"
        private const val COLOR_LED_YELLOW = "#FFDD00"
        private const val COLOR_FOOTER = "#181822"
        private const val COLOR_TEXT = "#808080"
    }
}

internal sealed class BoardState {
    data object Loading : BoardState()
    data class Data(
        val station: String,
        val rows: List<PreparedRow>,
        val hasAnimation: Boolean
    ) : BoardState()
    data class Error(val message: String) : BoardState()
}

internal data class PreparedRow(
    val name: String,
    val time: String,
    val isDue: Boolean,
    val nameWidth: Double,
    val timeWidth: Double,
    val maxNameWidth: Double
)
