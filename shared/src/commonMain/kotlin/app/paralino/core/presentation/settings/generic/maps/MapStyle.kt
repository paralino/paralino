package app.paralino.core.presentation.settings.generic.maps

sealed class MapStyle {
    abstract val key: String
    abstract val title: String
    open val themeAlias: List<String> = emptyList()
    abstract val isPremium: Boolean

    sealed class Light : MapStyle() {
        data object OPEN_FREE_MAP_LIBERTY : Light() {
            override val key: String = "ofm_liberty"
            override val title: String = "OpenFreeMap Liberty"
            override val themeAlias: List<String> = listOf("openfreemap")
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_BRIGHT : Light() {
            override val key: String = "ofm_bright"
            override val title: String = "OpenFreeMap Bright"
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_POSITRON : Light() {
            override val key: String = "ofm_positron"
            override val title: String = "OpenFreeMap Positron"
            override val isPremium: Boolean = false
        }

        data object MAPTILER_STREET_LIGHT : Light() {
            override val key: String = "maptiler_street_light"
            override val title: String = "MapTiler Street"
            override val themeAlias: List<String> = listOf("maptiler")
            override val isPremium: Boolean = true
        }

        data object MAPTILER_OUTDOOR_LIGHT : Light() {
            override val key: String = "maptiler_outdoor_light"
            override val title: String = "MapTiler Outdoor"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_DATAVIZ_LIGHT : Light() {
            override val key: String = "maptiler_dataviz_light"
            override val title: String = "MapTiler Dataviz"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_BASE_LIGHT : Light() {
            override val key: String = "maptiler_base_light"
            override val title: String = "MapTiler Base"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_BACKDROP_LIGHT : Light() {
            override val key: String = "maptiler_backdrop_light"
            override val title: String = "MapTiler Backdrop"
            override val isPremium: Boolean = true
        }

        companion object {
            val allLightStyles: List<MapStyle.Light> = listOf(
                MapStyle.Light.OPEN_FREE_MAP_LIBERTY,
                MapStyle.Light.OPEN_FREE_MAP_BRIGHT,
                MapStyle.Light.OPEN_FREE_MAP_POSITRON,
                MapStyle.Light.MAPTILER_BASE_LIGHT,
                MapStyle.Light.MAPTILER_STREET_LIGHT,
                MapStyle.Light.MAPTILER_DATAVIZ_LIGHT,
                MapStyle.Light.MAPTILER_BACKDROP_LIGHT,
                MapStyle.Light.MAPTILER_OUTDOOR_LIGHT,
            )
        }
    }

    sealed class Dark : MapStyle() {
        data object OPEN_FREE_MAP_DARK : Dark() {
            override val key: String = "ofm_dark"
            override val title: String = "OpenFreeMap Dark"
            override val themeAlias: List<String> = listOf("openfreemap")
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_FIORD : Dark() {
            override val key: String = "ofm_fiord"
            override val title: String = "OpenFreeMap Fiord"
            override val isPremium: Boolean = false
        }

        data object MAPTILER_STREET_DARK : Dark() {
            override val key: String = "maptiler_street_dark"
            override val title: String = "MapTiler Street"
            override val themeAlias: List<String> = listOf("maptiler")
            override val isPremium: Boolean = true
        }

        data object MAPTILER_OUTDOOR_DARK : Dark() {
            override val key: String = "maptiler_outdoor_dark"
            override val title: String = "MapTiler Outdoor"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_DATAVIZ_DARK : Dark() {
            override val key: String = "maptiler_dataviz_dark"
            override val title: String = "MapTiler Dataviz"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_BASE_DARK : Dark() {
            override val key: String = "maptiler_base_dark"
            override val title: String = "MapTiler Base"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_BACKDROP_DARK : Dark() {
            override val key: String = "maptiler_backdrop_dark"
            override val title: String = "MapTiler Backdrop"
            override val isPremium: Boolean = true
        }

        companion object {
            val allDarkStyles: List<MapStyle.Dark> = listOf(
                MapStyle.Dark.OPEN_FREE_MAP_DARK,
                MapStyle.Dark.OPEN_FREE_MAP_FIORD,
                MapStyle.Dark.MAPTILER_BASE_DARK,
                MapStyle.Dark.MAPTILER_STREET_DARK,
                MapStyle.Dark.MAPTILER_DATAVIZ_DARK,
                MapStyle.Dark.MAPTILER_BACKDROP_DARK,
                MapStyle.Dark.MAPTILER_OUTDOOR_DARK,
            )
        }
    }

    val allKeys get() = listOf(key).plus(themeAlias)

    companion object {
        fun lightStyleFromKey(key: String): Light {
            return MapStyle.Light.allLightStyles.firstOrNull { it.allKeys.contains(key) }
                ?: Light.OPEN_FREE_MAP_LIBERTY
        }

        fun darkStyleFromKey(key: String): Dark {
            return MapStyle.Dark.allDarkStyles.firstOrNull { it.allKeys.contains(key) } ?: Dark.OPEN_FREE_MAP_DARK
        }
    }
}