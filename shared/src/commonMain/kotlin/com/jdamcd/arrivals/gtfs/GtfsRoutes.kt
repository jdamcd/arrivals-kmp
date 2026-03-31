package com.jdamcd.arrivals.gtfs

internal class GtfsRoutes(
    routes: String,
    agencyExpressOverrides: Map<String, Map<String, String>> = emptyMap()
) {

    private val expressRoutes: Set<String>

    private val styles: Map<String, RouteStyle>

    init {
        val (header, rows) = parseCsvRows(routes)
        val parsed = parseRoutes(header, rows)
        val agencies = parseAgencies(header, rows)
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

private fun parseAgencies(header: List<String>, rows: List<List<String>>): Set<String> {
    val agencyIndex = header.indexOf("agency_id")
    if (agencyIndex < 0) return emptySet()
    return rows.mapNotNullTo(mutableSetOf()) { parts ->
        parts.getOrNull(agencyIndex)?.takeIf { it.isNotBlank() }
    }
}

private fun parseRoutes(header: List<String>, rows: List<List<String>>): Map<String, RouteStyle> {
    val idIndex = header.indexOf("route_id")
    val shortNameIndex = header.indexOf("route_short_name")
    val colorIndex = header.indexOf("route_color")
    val textColorIndex = header.indexOf("route_text_color")
    if (idIndex < 0 || colorIndex < 0) return emptyMap()
    return rows.mapNotNull { parts ->
        val id = parts.getOrNull(idIndex)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val color = parts.getOrNull(colorIndex)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val shortName = if (shortNameIndex >= 0) parts.getOrNull(shortNameIndex) else null
        val label = shortName?.takeIf { it.isNotBlank() }?.let {
            if (it.length > 3) it.first().toString() else it
        }
        val textColor = if (textColorIndex >= 0) {
            parts.getOrNull(textColorIndex)?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        id to RouteStyle(color = color, label = label, textColor = textColor)
    }.toMap()
}
