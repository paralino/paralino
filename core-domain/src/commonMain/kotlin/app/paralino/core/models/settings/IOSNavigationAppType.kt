package app.paralino.core.models.settings

import app.paralino.core.MR
import app.paralino.core.provider.ComposeUriHandler
import io.ktor.http.encodeURLParameter

enum class IOSNavigationAppType(val key: String, val scheme: String) {
    APPLE_MAPS(key = "apple", scheme = "maps://"),
    GOOGLE_MAPS(key = "google", scheme = "comgooglemaps://"),
    OSMAND(key = "osmand", scheme = "osmandmaps://"),
    WAZE(key = "waze", scheme = "waze://"),
    HERE_WE_GO(key = "herewego", scheme = "here-route://"),
    SYGIC(key = "sygic", scheme = "com.sygic.aura://"),
    CITYMAPPER(key = "citymapper", scheme = "citymapper://"),
    YANDEX_MAPS(key = "yandex", scheme = "yandexmaps://"),
    MOOVIT(key = "moovit", scheme = "moovit://");

    fun buildLaunchURL(
        lat: Double,
        lng: Double,
        title: String?
    ): String {
        val encodedTitle = title?.encodeURLParameter()
        return when (this) {
            APPLE_MAPS -> {
                // Apple Maps: maps://?ll=lat,lng&q=Title
                val q = encodedTitle?.let { "&q=$it" } ?: ""
                "maps://?ll=$lat,$lng$q"
            }
            GOOGLE_MAPS -> {
                // Google Maps: comgooglemaps://?q=lat,lng(Label)
                val label = encodedTitle?.let { "($it)" } ?: ""
                "comgooglemaps://?q=$lat,$lng$label"
            }
            OSMAND -> {
                // OsmAnd: osmandmaps://?lat=...&lon=...&title=...
                val t = encodedTitle?.let { "&title=$it" } ?: ""
                "osmandmaps://?lat=$lat&lon=$lng$t"
            }
            WAZE -> {
                // Waze: waze://?ll=lat,lng&navigate=yes
                "waze://?ll=$lat,$lng&navigate=yes"
            }
            HERE_WE_GO -> {
                // HERE WeGo: here-route://mylocation/lat,lng
                "here-route://mylocation/$lat,$lng"
            }
            SYGIC -> {
                // Sygic: com.sygic.aura://coordinate|lat|lng|drive
                "com.sygic.aura://coordinate|$lat|$lng|drive"
            }
            CITYMAPPER -> {
                // Citymapper: citymapper://directions?endcoord=lat,lng&endname=Title
                val name = encodedTitle?.let { "&endname=$it" } ?: ""
                "citymapper://directions?endcoord=$lat,$lng$name"
            }
            YANDEX_MAPS -> {
                // Yandex.Maps: yandexmaps://maps.yandex.com/?pt=lng,lat&z=12&l=map
                "yandexmaps://maps.yandex.com/?pt=$lng,$lat&z=12&l=map"
            }
            MOOVIT -> {
                // Moovit: moovit://directions?dest_lat=lat&dest_lon=lng&dest_name=Title
                val name = encodedTitle?.let { "&dest_name=$it" } ?: ""
                "moovit://directions?dest_lat=$lat&dest_lon=$lng$name"
            }
        }
    }

    val title get() = when(this) {
        APPLE_MAPS -> MR.strings.setting_navigationApp_appleMaps_title
        GOOGLE_MAPS -> MR.strings.setting_navigationApp_googleMaps_title
        OSMAND -> MR.strings.setting_navigationApp_osmand_title
        WAZE -> MR.strings.setting_navigationApp_waze_title
        HERE_WE_GO -> MR.strings.setting_navigationApp_hereWeGo_title
        SYGIC -> MR.strings.setting_navigationApp_sygic_title
        CITYMAPPER -> MR.strings.setting_navigationApp_citymapper_title
        YANDEX_MAPS -> MR.strings.setting_navigationApp_yandex_title
        MOOVIT -> MR.strings.setting_navigationApp_moovit_title
    }

    companion object {
        fun fromKey(key: String): IOSNavigationAppType? {
            return entries.firstOrNull { it.key == key }
        }

        val default: IOSNavigationAppType get() {
            return supportedMaps().let {
                it.firstOrNull { it == APPLE_MAPS } ?: it.firstOrNull()
            } ?: APPLE_MAPS
        }

        fun supportedMaps(): List<IOSNavigationAppType> {
            return entries.filter {
                ComposeUriHandler.isUrlSchemeSupported(scheme = it.scheme)
            }
        }
    }
}