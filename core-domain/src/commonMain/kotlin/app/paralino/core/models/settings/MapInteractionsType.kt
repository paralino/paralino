package app.paralino.core.models.settings

import androidx.compose.ui.unit.Dp
import app.paralino.core.resources.moko.*

enum class MapInteractionsType(val key: String) {
    DEVICE_DEFAULT("device_default"),
    NONE("none"),
    MINIMIZE("minimize"),
    COLLAPSED("collapsed");

    val title get() = when(this) {
        DEVICE_DEFAULT -> MR.strings.setting_mapInteractions_deviceDefault_title
        NONE -> MR.strings.setting_mapInteractions_none_title
        MINIMIZE -> MR.strings.setting_mapInteractions_minimize_title
        COLLAPSED -> MR.strings.setting_mapInteractions_collapsed_title
    }

    val description get() = when(this) {
        DEVICE_DEFAULT -> MR.strings.setting_mapInteractions_deviceDefault_info
        NONE -> MR.strings.setting_mapInteractions_none_info
        MINIMIZE -> MR.strings.setting_mapInteractions_minimize_info
        COLLAPSED -> MR.strings.setting_mapInteractions_collapsed_info
    }

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }

        fun forScreenSize(
            defaultType: MapInteractionsType,
            maxWidthDp: Dp,
            maxHeightDp: Dp
        ): MapInteractionsType {
            return when (defaultType) {
                MapInteractionsType.DEVICE_DEFAULT -> {
                    when {
                        maxHeightDp.value < 460 -> {
                            MapInteractionsType.COLLAPSED
                        }

                        maxHeightDp.value < 800 -> {
                            MapInteractionsType.MINIMIZE
                        }

                        else -> {
                            MapInteractionsType.NONE
                        }
                    }
                }

                else -> defaultType
            }
        }
    }
}