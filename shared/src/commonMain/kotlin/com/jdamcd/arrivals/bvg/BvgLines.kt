package com.jdamcd.arrivals.bvg

import com.jdamcd.arrivals.LineBadge

internal object BvgLines {

    private data class LineColor(val color: String, val textColor: String? = null)

    private val colors = mapOf(
        // U-Bahn
        "U1" to LineColor("7DAD4C"),
        "U2" to LineColor("DA421E"),
        "U3" to LineColor("16683D"),
        "U4" to LineColor("F0D722", "000000"),
        "U5" to LineColor("7E5330"),
        "U6" to LineColor("8C6DAB"),
        "U7" to LineColor("009BD5"),
        "U8" to LineColor("224F86"),
        "U9" to LineColor("F3791D"),
        // S-Bahn
        "S1" to LineColor("DA6BA2"),
        "S2" to LineColor("007734"),
        "S3" to LineColor("0066AD"),
        "S5" to LineColor("EB7405"),
        "S7" to LineColor("816DA6"),
        "S8" to LineColor("66AA22"),
        "S9" to LineColor("992746"),
        "S25" to LineColor("007734"),
        "S26" to LineColor("007734"),
        "S41" to LineColor("AD5937"),
        "S42" to LineColor("CB6418"),
        "S45" to LineColor("CA9A55"),
        "S46" to LineColor("CA9A55"),
        "S47" to LineColor("CA9A55"),
        "S75" to LineColor("816DA6"),
        "S85" to LineColor("66AA22")
    )

    private val tramColor = LineColor("BE1414")

    fun badgeFor(lineName: String): LineBadge? {
        val lineColor = colors[lineName]
            ?: if (lineName.startsWith("M") || lineName.all { it.isDigit() }) tramColor else return null
        return LineBadge(label = lineName, color = lineColor.color, textColor = lineColor.textColor)
    }
}
