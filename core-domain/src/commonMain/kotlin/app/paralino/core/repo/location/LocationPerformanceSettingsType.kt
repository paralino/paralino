package app.paralino.core.repo.location

import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.current
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

sealed interface LocationPerformanceSettingsType {
    val key: String
    val updateInterval: Duration
    val minUpdateInterval: Duration
    val minUpdateDistanceMeters: Int
    val isPremiumFeature: Boolean

    sealed class HighPerformance : LocationPerformanceSettingsType {
        override val updateInterval: Duration = 3.seconds
        override val minUpdateInterval: Duration = 2.seconds
        override val minUpdateDistanceMeters: Int = 0
        abstract val allowedVeryHighPerformanceDuration: Duration
        abstract val allowedHighPerformanceDuration: Duration
        abstract val allowedMediumPerformanceDuration: Duration

        data object Level1 : HighPerformance() {
            override val allowedVeryHighPerformanceDuration: Duration = 3.minutes
            override val allowedHighPerformanceDuration: Duration = 3.minutes
            override val allowedMediumPerformanceDuration: Duration = 6.minutes
            override val isPremiumFeature: Boolean = false
            override val key: String = "high_performance_100"
        }

        data object Level2 : HighPerformance() {
            override val allowedVeryHighPerformanceDuration: Duration = 5.minutes
            override val allowedHighPerformanceDuration: Duration = 6.minutes
            override val allowedMediumPerformanceDuration: Duration = 12.minutes
            override val isPremiumFeature: Boolean = true
            override val key: String = "high_performance_200"
        }

        data object Level3 : HighPerformance() {
            override val allowedVeryHighPerformanceDuration: Duration = 7.minutes
            override val allowedHighPerformanceDuration: Duration = 12.minutes
            override val allowedMediumPerformanceDuration: Duration = 18.minutes
            override val isPremiumFeature: Boolean = true
            override val key: String = "high_performance_300"
        }

        data object LevelMax : HighPerformance() {
            override val allowedVeryHighPerformanceDuration: Duration = 10.minutes
            override val allowedHighPerformanceDuration: Duration = 20.minutes
            override val allowedMediumPerformanceDuration: Duration = 20.minutes
            override val isPremiumFeature: Boolean = true
            override val key: String = "high_performance_400"
        }

        companion object {
            val default: HighPerformance = Level1
            val defaultPremium: HighPerformance = Level2
            fun fromKey(key: String?): HighPerformance {
                return when (key) {
                    Level1.key -> Level1
                    Level2.key -> Level2
                    Level3.key -> Level3
                    LevelMax.key -> LevelMax
                    else -> default
                }
            }
        }
    }

    sealed interface BatterySaver : LocationPerformanceSettingsType {
        data object Level1 : BatterySaver {
            override val updateInterval: Duration = 6.hours
            override val minUpdateInterval: Duration = 2.hours
            override val minUpdateDistanceMeters: Int = 1000
            override val isPremiumFeature: Boolean = false
            override val key: String = "battery_level_100"
        }

        data object Level2 : BatterySaver {
            override val updateInterval: Duration = 3.hours
            override val minUpdateInterval: Duration = 1.hours
            override val minUpdateDistanceMeters: Int = 500
            override val isPremiumFeature: Boolean = false
            override val key: String = "battery_level_200"
        }

        data object Level3 : BatterySaver {
            override val updateInterval: Duration = 1.hours
            override val minUpdateInterval: Duration = 20.minutes
            override val minUpdateDistanceMeters: Int = 200
            override val isPremiumFeature: Boolean = true
            override val key: String = "battery_level_300"
        }

        companion object {
            val default: BatterySaver = Level1
            fun fromKey(key: String?): BatterySaver {
                return when (key) {
                    Level1.key -> Level1
                    Level2.key -> Level2
                    Level3.key -> Level3
                    else -> default
                }
            }
        }
    }

    sealed interface Balanced : LocationPerformanceSettingsType {
        data object Level1 : Balanced {
            override val updateInterval: Duration = 4.hours
            override val minUpdateInterval: Duration = 2.hours
            override val minUpdateDistanceMeters: Int = 1000
            override val isPremiumFeature: Boolean = false
            override val key: String = "balanced_level_100"
        }

        data object Level2 : Balanced {
            override val updateInterval: Duration = 2.hours
            override val minUpdateInterval: Duration = 40.minutes
            override val minUpdateDistanceMeters: Int = 500
            override val isPremiumFeature: Boolean = false
            override val key: String = "balanced_level_200"
        }

        data object Level3 : Balanced {
            override val updateInterval: Duration = 1.hours
            override val minUpdateInterval: Duration = 20.minutes
            override val minUpdateDistanceMeters: Int = 200
            override val isPremiumFeature: Boolean = false
            override val key: String = "balanced_level_300"
        }

        data object Level4 : Balanced {
            override val updateInterval: Duration = 20.minutes
            override val minUpdateInterval: Duration = 3.minutes
            override val minUpdateDistanceMeters: Int = 70
            override val isPremiumFeature: Boolean = true
            override val key: String = "balanced_level_400"
        }

        data object Level5 : Balanced {
            override val updateInterval: Duration = 5.minutes
            override val minUpdateInterval: Duration = 1.minutes
            override val minUpdateDistanceMeters: Int get() = when (DeviceType.current) {
                DeviceType.Android -> 10
                DeviceType.iOS -> 0
            }
            override val isPremiumFeature: Boolean = true
            override val key: String = "balanced_level_500"
        }

        companion object {
            val default: Balanced = Level3
            fun fromKey(key: String?): Balanced {
                return when (key) {
                    Level1.key -> Level1
                    Level2.key -> Level2
                    Level3.key -> Level3
                    Level4.key -> Level4
                    Level5.key -> Level5
                    else -> default
                }
            }
        }
    }


    sealed class History : LocationPerformanceSettingsType {
        abstract val speedThresholdMs: Float
        abstract val elevationThreshold: Int
        abstract val accuracyThreshold: Int
        override val updateInterval: Duration = 3.seconds
        override val minUpdateInterval: Duration = 2.seconds
        override val isPremiumFeature: Boolean = false

        data object Level1 : History() {
            override val minUpdateDistanceMeters: Int = 80
            override val accuracyThreshold: Int = 60
            override val elevationThreshold: Int = 30
            override val speedThresholdMs: Float = 1f
            override val key: String = "history_100"
        }

        data object Level2 : History() {
            override val minUpdateDistanceMeters: Int = 20
            override val accuracyThreshold: Int = 20
            override val elevationThreshold: Int = 20
            override val speedThresholdMs: Float = 0.5f
            override val key: String = "history_200"
        }

        data object Level3 : History() {
            override val minUpdateDistanceMeters: Int = 10
            override val accuracyThreshold: Int = 5
            override val elevationThreshold: Int = 10
            override val speedThresholdMs: Float = 0.1f
            override val key: String = "history_300"
        }

        companion object {
            val default: History = Level2
            fun fromKey(key: String?): History {
                return when (key) {
                    Level1.key -> Level1
                    Level2.key -> Level2
                    Level3.key -> Level3
                    else -> default
                }
            }
        }
    }
}
