package com.jdamcd.arrivals.gtfs

import com.jdamcd.arrivals.gtfs.csv.parseCsvRows

internal class GtfsRoutes(
    routes: String,
    agencyExpressOverrides: Map<String, Map<String, String>> = emptyMap()
) {

    private val expressRoutes: Set<String>

    private val styles: Map<String, RouteStyle>

    init {
        val (header, rows) = parseCsvRows(routes)
        val (parsed, agencies) = parseRoutes(header, rows)
        val expressOverrides = agencies.firstNotNullOfOrNull { agencyExpressOverrides[it] } ?: emptyMap()
        expressRoutes = expressOverrides.keys
        styles = buildMap {
            putAll(parsed)
            for ((routeId, baseRoute) in expressOverrides) {
                val baseStyle = get(baseRoute)
                val style = get(routeId) ?: baseStyle ?: continue
                put(routeId, style.copy(label = baseStyle?.label ?: style.label))
            }
        }
    }

    fun styleFor(routeId: String?): RouteStyle? = styles[routeId]

    fun isExpress(routeId: String?): Boolean = routeId in expressRoutes
}

private fun parseRoutes(
    header: List<String>,
    rows: List<List<String>>
): Pair<Map<String, RouteStyle>, Set<String>> {
    val idIndex = header.indexOf("route_id")
    val shortNameIndex = header.indexOf("route_short_name")
    val colorIndex = header.indexOf("route_color")
    val textColorIndex = header.indexOf("route_text_color")
    val agencyIndex = header.indexOf("agency_id")
    if (idIndex < 0 || colorIndex < 0) return emptyMap<String, RouteStyle>() to emptySet()
    val styles = mutableMapOf<String, RouteStyle>()
    val agencies = mutableSetOf<String>()
    for (parts in rows) {
        if (agencyIndex >= 0) {
            parts.getOrNull(agencyIndex)?.takeIf { it.isNotBlank() }?.let { agencies.add(it) }
        }
        val id = parts.getOrNull(idIndex)?.takeIf { it.isNotBlank() } ?: continue
        val color = parts.getOrNull(colorIndex)?.takeIf { it.isNotBlank() } ?: continue
        val shortName = if (shortNameIndex >= 0) parts.getOrNull(shortNameIndex) else null
        val label = formatLabel(shortName)
        val textColor = if (textColorIndex >= 0) {
            parts.getOrNull(textColorIndex)?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        styles[id] = RouteStyle(color = color, label = label, textColor = textColor)
    }
    return styles to agencies
}

private fun formatLabel(shortName: String?): String? = shortName?.takeIf { it.isNotBlank() }?.let {
    if (it.length > 3) it.first().toString() else it
}
