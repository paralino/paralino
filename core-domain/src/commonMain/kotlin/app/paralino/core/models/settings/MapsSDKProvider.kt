package app.paralino.core.models.settings

import app.paralino.core.resources.moko.*
import dev.icerock.moko.resources.StringResource

sealed interface MapsSDKProvider {
    val key: String
    val title: StringResource
    val description: StringResource
    val tag: StringResource?
    val hasSatelliteMaps: Boolean
    val isPremium: Boolean

    data object GOOGLE : MapsSDKProvider {
        override val key: String = "google_maps"
        override val title = MR.strings.setting_mapsProvider_googleMaps_title
        override val description = MR.strings.setting_mapsProvider_googleMaps_info
        override val tag = null
        override val hasSatelliteMaps = true
        override val isPremium = false
    }

    data object NONE : MapsSDKProvider {
        override val key: String = "none"
        override val title = MR.strings.setting_mapsProvider_none_title
        override val description = MR.strings.setting_mapsProvider_none_info
        override val tag = null
        override val hasSatelliteMaps = false
        override val isPremium = false
    }

    sealed interface MAPLIBRE : MapsSDKProvider {
        val lightStyle: MapStyle.Light
        val darkStyle: MapStyle.Dark

        fun lightStyleUri(isSatelliteMaps: Boolean): String
        fun darkStyleUri(isSatelliteMaps: Boolean): String

        data class MAPLIBRE_FREE(
            override val lightStyle: MapStyle.Light = MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
            override val darkStyle: MapStyle.Dark = MapStyle.Dark.OPEN_FREE_MAP_DARK
        ) : MAPLIBRE {
            override val key: String = "maplibre_free"
            override val title = MR.strings.setting_mapsProvider_mapLibreFree_title
            override val description = MR.strings.setting_mapsProvider_mapLibreFree_info
            override val tag = MR.strings.setting_mapsProvider_mapLibreFree_tag
            override val hasSatelliteMaps = false
            override val isPremium = false

            override fun lightStyleUri(isSatelliteMaps: Boolean): String {
                return when (lightStyle) {
                    MapStyle.Light.MAPTILER_DATAVIZ_LIGHT,
                    MapStyle.Light.MAPTILER_OUTDOOR_LIGHT,
                    MapStyle.Light.MAPTILER_STREET_LIGHT,
                    MapStyle.Light.MAPTILER_BACKDROP_LIGHT,
                    MapStyle.Light.MAPTILER_BASE_LIGHT,
                    MapStyle.Light.OPEN_FREE_MAP_LIBERTY -> "https://tiles.openfreemap.org/styles/liberty"
                    MapStyle.Light.OPEN_FREE_MAP_BRIGHT -> "https://tiles.openfreemap.org/styles/bright"
                    MapStyle.Light.OPEN_FREE_MAP_POSITRON -> "https://tiles.openfreemap.org/styles/positron"
                }
            }

            override fun darkStyleUri(isSatelliteMaps: Boolean): String {
                return when (darkStyle) {
                    MapStyle.Dark.MAPTILER_DATAVIZ_DARK,
                    MapStyle.Dark.MAPTILER_OUTDOOR_DARK,
                    MapStyle.Dark.MAPTILER_STREET_DARK,
                    MapStyle.Dark.MAPTILER_BACKDROP_DARK,
                    MapStyle.Dark.MAPTILER_BASE_DARK,
                    MapStyle.Dark.OPEN_FREE_MAP_DARK -> "https://tiles.openfreemap.org/styles/dark"
                    MapStyle.Dark.OPEN_FREE_MAP_FIORD -> "https://tiles.openfreemap.org/styles/fiord"
                }
            }
        }

        data class MAPLIBRE_PREMIUM(
            val mapTilerStyleToken: String,
            val mapTilerSatelliteToken: String,
            override val lightStyle: MapStyle.Light = MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
            override val darkStyle: MapStyle.Dark = MapStyle.Dark.OPEN_FREE_MAP_DARK
        ) : MAPLIBRE {
            override val key: String = "maplibre_premium"
            override val title = MR.strings.setting_mapsProvider_mapLibrePremium_title
            override val description = MR.strings.setting_mapsProvider_mapLibrePremium_info
            override val tag = null
            override val hasSatelliteMaps = mapTilerSatelliteToken.isNotBlank()
            override val isPremium = true

            override fun lightStyleUri(isSatelliteMaps: Boolean): String {
                if (isSatelliteMaps) {
                    return "https://api.maptiler.com/maps/hybrid-v4/style.json?key=${mapTilerSatelliteToken}"
                }

                return when (lightStyle) {
                    MapStyle.Light.MAPTILER_DATAVIZ_LIGHT -> "https://api.maptiler.com/maps/dataviz-v4/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Light.MAPTILER_OUTDOOR_LIGHT -> "https://api.maptiler.com/maps/outdoor-v4/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Light.MAPTILER_STREET_LIGHT -> "https://api.maptiler.com/maps/streets-v4/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Light.OPEN_FREE_MAP_LIBERTY -> "https://tiles.openfreemap.org/styles/liberty"
                    MapStyle.Light.OPEN_FREE_MAP_BRIGHT -> "https://tiles.openfreemap.org/styles/bright"
                    MapStyle.Light.OPEN_FREE_MAP_POSITRON -> "https://tiles.openfreemap.org/styles/positron"
                    MapStyle.Light.MAPTILER_BACKDROP_LIGHT -> "https://api.maptiler.com/maps/backdrop-v4-light/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Light.MAPTILER_BASE_LIGHT -> "https://api.maptiler.com/maps/base-v4-light/style.json?key=${mapTilerStyleToken}"
                }
            }

            override fun darkStyleUri(isSatelliteMaps: Boolean): String {
                if (isSatelliteMaps) {
                    return "https://api.maptiler.com/maps/hybrid-v4/style.json?key=${mapTilerSatelliteToken}"
                }

                return when (darkStyle) {
                    MapStyle.Dark.MAPTILER_DATAVIZ_DARK -> "https://api.maptiler.com/maps/dataviz-v4-dark/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Dark.MAPTILER_OUTDOOR_DARK -> "https://api.maptiler.com/maps/outdoor-v4-dark/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Dark.MAPTILER_STREET_DARK -> "https://api.maptiler.com/maps/streets-v4-dark/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Dark.OPEN_FREE_MAP_DARK -> "https://tiles.openfreemap.org/styles/dark"
                    MapStyle.Dark.OPEN_FREE_MAP_FIORD -> "https://tiles.openfreemap.org/styles/fiord"
                    MapStyle.Dark.MAPTILER_BACKDROP_DARK -> "https://api.maptiler.com/maps/backdrop-v4-dark/style.json?key=${mapTilerStyleToken}"
                    MapStyle.Dark.MAPTILER_BASE_DARK -> "https://api.maptiler.com/maps/base-v4-dark/style.json?key=${mapTilerStyleToken}"
                }
            }
        }
    }

    companion object {
        fun fromKey(
            key: String,
            mapTilerStyleToken: String,
            mapTilerSatelliteToken: String,
            lightStyle: MapStyle.Light = MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
            darkStyle: MapStyle.Dark = MapStyle.Dark.OPEN_FREE_MAP_DARK
        ): MapsSDKProvider? {
            return when (key) {
                GOOGLE.key -> GOOGLE
                NONE.key -> NONE

                MAPLIBRE.MAPLIBRE_FREE().key -> MAPLIBRE.MAPLIBRE_FREE(
                    lightStyle = lightStyle.takeIf { !it.isPremium }
                        ?: MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
                    darkStyle = darkStyle.takeIf { !it.isPremium }
                        ?: MapStyle.Dark.OPEN_FREE_MAP_DARK
                )

                MAPLIBRE.MAPLIBRE_PREMIUM(
                    mapTilerStyleToken = mapTilerStyleToken,
                    mapTilerSatelliteToken = mapTilerSatelliteToken
                ).key -> {
                    MAPLIBRE.MAPLIBRE_PREMIUM(
                        mapTilerStyleToken = mapTilerStyleToken,
                        mapTilerSatelliteToken = mapTilerSatelliteToken,
                        lightStyle = lightStyle,
                        darkStyle = darkStyle
                    )
                }

                else -> null
            }
        }
    }
}