package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.LineBadge

internal object TflLines {

    private data class LineInfo(val label: String, val color: String, val textColor: String? = null)

    private val lines = mapOf(
        "bakerloo" to LineInfo("BAK", "B36305"),
        "central" to LineInfo("CEN", "E32017"),
        "circle" to LineInfo("CIR", "FFD300", "000000"),
        "district" to LineInfo("DST", "00782A"),
        "hammersmith-city" to LineInfo("H&C", "F3A9BB", "000000"),
        "jubilee" to LineInfo("JUB", "A0A5A9", "000000"),
        "metropolitan" to LineInfo("MET", "9B0056"),
        "northern" to LineInfo("NOR", "000000"),
        "piccadilly" to LineInfo("PIC", "003688"),
        "victoria" to LineInfo("VIC", "0098D4"),
        "waterloo-city" to LineInfo("W&C", "95CDBA", "000000"),
        "dlr" to LineInfo("DLR", "00A4A7"),
        "elizabeth" to LineInfo("ELZ", "6950A1"),
        "liberty" to LineInfo("LIB", "606667"),
        "lioness" to LineInfo("LIO", "EF9600"),
        "mildmay" to LineInfo("MIL", "2774AE"),
        "suffragette" to LineInfo("SUF", "5BA763"),
        "weaver" to LineInfo("WEA", "893B67"),
        "windrush" to LineInfo("WIN", "D22730"),
        "tram" to LineInfo("TRM", "84B817", "000000")
    )

    fun badgeFor(lineId: String): LineBadge? {
        val info = lines[lineId] ?: return null
        return LineBadge(label = info.label, color = info.color, textColor = info.textColor)
    }
}
