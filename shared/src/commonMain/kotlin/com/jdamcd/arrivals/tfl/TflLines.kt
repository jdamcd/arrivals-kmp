package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.LineBadge

internal object TflLines {

    private data class LineInfo(val label: String, val color: String, val textColor: String? = null)

    private val lines = mapOf(
        "bakerloo" to LineInfo("B", "B36305"),
        "central" to LineInfo("C", "E32017"),
        "circle" to LineInfo("CIR", "FFD300", "000000"),
        "district" to LineInfo("D", "00782A"),
        "hammersmith-city" to LineInfo("HC", "F3A9BB", "000000"),
        "jubilee" to LineInfo("J", "A0A5A9", "000000"),
        "metropolitan" to LineInfo("M", "9B0056"),
        "northern" to LineInfo("N", "000000"),
        "piccadilly" to LineInfo("P", "003688"),
        "victoria" to LineInfo("V", "0098D4"),
        "waterloo-city" to LineInfo("WC", "95CDBA", "000000"),
        "dlr" to LineInfo("DLR", "00A4A7"),
        "elizabeth" to LineInfo("E", "6950A1"),
        "liberty" to LineInfo("O", "606667"),
        "lioness" to LineInfo("O", "EF9600"),
        "mildmay" to LineInfo("O", "2774AE"),
        "suffragette" to LineInfo("O", "5BA763"),
        "weaver" to LineInfo("O", "893B67"),
        "windrush" to LineInfo("O", "D22730"),
        "tram" to LineInfo("T", "84B817", "000000")
    )

    fun badgeFor(lineId: String): LineBadge? {
        val info = lines[lineId] ?: return null
        return LineBadge(label = info.label, color = info.color, textColor = info.textColor)
    }
}
