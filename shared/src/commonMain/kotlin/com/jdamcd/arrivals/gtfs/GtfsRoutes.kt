package com.jdamcd.arrivals.gtfs

internal class GtfsRoutes(
    routes: String,
    expressOverrides: Map<String, String> = emptyMap()
) {

    private val expressRoutes: Set<String> = expressOverrides.keys

    private val styles: Map<String, RouteStyle> = buildMap {
        putAll(parseRoutes(routes))
        for ((routeId, baseRoute) in expressOverrides) {
            val baseStyle = get(baseRoute)
            val style = get(routeId) ?: baseStyle ?: continue
            put(routeId, style.copy(label = baseStyle?.label ?: style.label))
        }
    }

    fun styleFor(routeId: String?): RouteStyle? = styles[routeId]

    fun isExpress(routeId: String?): Boolean = routeId in expressRoutes
}

private fun parseRoutes(routes: String): Map<String, RouteStyle> = routes
    .split("\n")
    .filter { it.isNotBlank() }
    .let { lines ->
        if (lines.isEmpty()) return emptyMap()
        val header = parseCsvLine(lines.first())
        val idIndex = header.indexOf("route_id")
        val shortNameIndex = header.indexOf("route_short_name")
        val colorIndex = header.indexOf("route_color")
        val textColorIndex = header.indexOf("route_text_color")
        if (idIndex < 0 || colorIndex < 0) return emptyMap()
        lines
            .drop(1)
            .mapNotNull { line ->
                val parts = parseCsvLine(line)
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
            }
            .toMap()
    }
