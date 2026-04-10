package app.paralino.core.presentation.settings.generic.maps

sealed class MapStyle {
    abstract val key: String
    open val themeAlias: List<String> = emptyList()
    abstract val isPremium: Boolean

    sealed class Light : MapStyle() {
        data object OPEN_FREE_MAP_LIBERTY : Light() {
            override val key: String = "ofm_liberty"
            override val themeAlias: List<String> = listOf("openfreemap")
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_BRIGHT : Light() {
            override val key: String = "ofm_bright"
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_POSITRON : Light() {
            override val key: String = "ofm_positron"
            override val isPremium: Boolean = false
        }

        data object MAPTILER_STREET_LIGHT : Light() {
            override val key: String = "maptiler_street_light"
            override val themeAlias: List<String> = listOf("maptiler")
            override val isPremium: Boolean = true
        }

        data object MAPTILER_OUTDOOR_LIGHT : Light() {
            override val key: String = "maptiler_outdoor_light"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_DATAVIZ_LIGHT : Light() {
            override val key: String = "maptiler_dataviz_light"
            override val isPremium: Boolean = true
        }
    }

    sealed class Dark : MapStyle() {
        data object OPEN_FREE_MAP_DARK : Dark() {
            override val key: String = "ofm_dark"
            override val themeAlias: List<String> = listOf("openfreemap")
            override val isPremium: Boolean = false
        }

        data object OPEN_FREE_MAP_FIORD : Dark() {
            override val key: String = "ofm_fiord"
            override val isPremium: Boolean = false
        }

        data object MAPTILER_STREET_DARK : Dark() {
            override val key: String = "maptiler_street_dark"
            override val themeAlias: List<String> = listOf("maptiler")
            override val isPremium: Boolean = true
        }

        data object MAPTILER_OUTDOOR_DARK : Dark() {
            override val key: String = "maptiler_outdoor_dark"
            override val isPremium: Boolean = true
        }

        data object MAPTILER_DATAVIZ_DARK : Dark() {
            override val key: String = "maptiler_dataviz_dark"
            override val isPremium: Boolean = true
        }
    }

    val allKeys get() = listOf(key).plus(themeAlias)

    val title: String
        get() = when (this) {
            is Light.OPEN_FREE_MAP_LIBERTY -> "OpenFreeMap Liberty"
            is Light.OPEN_FREE_MAP_BRIGHT -> "OpenFreeMap Bright"
            is Light.OPEN_FREE_MAP_POSITRON -> "OpenFreeMap Positron"
            is Dark.OPEN_FREE_MAP_DARK -> "OpenFreeMap Dark"
            is Dark.OPEN_FREE_MAP_FIORD -> "OpenFreeMap Fiord"
            is Light.MAPTILER_STREET_LIGHT -> "MapTiler Street"
            is Dark.MAPTILER_STREET_DARK -> "MapTiler Street"
            is Light.MAPTILER_OUTDOOR_LIGHT -> "MapTiler Outdoor"
            is Dark.MAPTILER_OUTDOOR_DARK -> "MapTiler Outdoor"
            is Light.MAPTILER_DATAVIZ_LIGHT -> "MapTiler Dataviz"
            is Dark.MAPTILER_DATAVIZ_DARK -> "MapTiler Dataviz"
        }

    companion object {

        val allLightStyles = listOf(
            Light.OPEN_FREE_MAP_LIBERTY,
            Light.OPEN_FREE_MAP_BRIGHT,
            Light.OPEN_FREE_MAP_POSITRON,
            Light.MAPTILER_STREET_LIGHT,
            Light.MAPTILER_DATAVIZ_LIGHT,
            Light.MAPTILER_OUTDOOR_LIGHT,
        )

        val allDarkStyles = listOf(
            Dark.OPEN_FREE_MAP_DARK,
            Dark.OPEN_FREE_MAP_FIORD,
            Dark.MAPTILER_STREET_DARK,
            Dark.MAPTILER_DATAVIZ_DARK,
            Dark.MAPTILER_OUTDOOR_DARK,
        )


        fun lightStyleFromKey(key: String): Light {
            return allLightStyles.firstOrNull { it.allKeys.contains(key) }
                ?: Light.OPEN_FREE_MAP_LIBERTY
        }

        fun darkStyleFromKey(key: String): Dark {
            return allDarkStyles.firstOrNull { it.allKeys.contains(key) } ?: Dark.OPEN_FREE_MAP_DARK
        }
    }
}