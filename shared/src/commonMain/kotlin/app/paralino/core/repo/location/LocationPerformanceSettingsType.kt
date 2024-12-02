package app.paralino.core.repo.location

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
        abstract val maxAllowedDuration: Duration

        data class Level1(
            override val maxAllowedDuration: Duration = 10.minutes,
            override val isPremiumFeature: Boolean = false
        ) : HighPerformance() {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "high_performance_100"
            }
        }

        data class Level2(
            override val maxAllowedDuration: Duration = 20.minutes,
            override val isPremiumFeature: Boolean = false
        ) : HighPerformance() {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "high_performance_200"
            }
        }

        data class Level3(
            override val maxAllowedDuration: Duration = 30.minutes,
            override val isPremiumFeature: Boolean = true
        ) : HighPerformance() {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "high_performance_300"
            }
        }

        data class LevelMax(
            override val maxAllowedDuration: Duration = 60.minutes,
            override val isPremiumFeature: Boolean = true
        ) : HighPerformance() {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "high_performance_400"
            }
        }

        companion object {
            val default: HighPerformance = Level2()
            fun fromKey(key: String?): HighPerformance {
                return when (key) {
                    Level1.key -> Level1()
                    Level2.key -> Level2()
                    Level3.key -> Level3()
                    LevelMax.key -> LevelMax()
                    else -> default
                }
            }
        }
    }

    sealed interface BatterySaver : LocationPerformanceSettingsType {
        data class Level1(
            override val updateInterval: Duration = 6.hours,
            override val minUpdateInterval: Duration = 2.hours,
            override val minUpdateDistanceMeters: Int = 1000,
            override val isPremiumFeature: Boolean = false
        ) : BatterySaver {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "battery_level_100"
            }
        }

        data class Level2(
            override val updateInterval: Duration = 3.hours,
            override val minUpdateInterval: Duration = 1.hours,
            override val minUpdateDistanceMeters: Int = 500,
            override val isPremiumFeature: Boolean = false
        ) : BatterySaver {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "battery_level_200"
            }
        }

        data class Level3(
            override val updateInterval: Duration = 1.hours,
            override val minUpdateInterval: Duration = 20.minutes,
            override val minUpdateDistanceMeters: Int = 200,
            override val isPremiumFeature: Boolean = true
        ) : BatterySaver {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "battery_level_300"
            }
        }

        companion object {
            val default: BatterySaver = Level1()
            fun fromKey(key: String?): BatterySaver {
                return when (key) {
                    Level1.key -> Level1()
                    Level2.key -> Level2()
                    Level3.key -> Level3()
                    else -> default
                }
            }
        }
    }

    sealed interface Balanced : LocationPerformanceSettingsType {
        data class Level1(
            override val updateInterval: Duration = 4.hours,
            override val minUpdateInterval: Duration = 2.hours,
            override val minUpdateDistanceMeters: Int = 1000,
            override val isPremiumFeature: Boolean = false
        ) : Balanced {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "balanced_level_100"
            }
        }

        data class Level2(
            override val updateInterval: Duration = 2.hours,
            override val minUpdateInterval: Duration = 40.minutes,
            override val minUpdateDistanceMeters: Int = 500,
            override val isPremiumFeature: Boolean = false
        ) : Balanced {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "balanced_level_200"
            }
        }

        data class Level3(
            override val updateInterval: Duration = 1.hours,
            override val minUpdateInterval: Duration = 20.minutes,
            override val minUpdateDistanceMeters: Int = 200,
            override val isPremiumFeature: Boolean = false
        ) : Balanced {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "balanced_level_300"
            }
        }

        data class Level4(
            override val updateInterval: Duration = 30.minutes,
            override val minUpdateInterval: Duration = 10.minutes,
            override val minUpdateDistanceMeters: Int = 100,
            override val isPremiumFeature: Boolean = true
        ) : Balanced {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "balanced_level_400"
            }
        }

        data class Level5(
            override val updateInterval: Duration = 30.minutes,
            override val minUpdateInterval: Duration = 2.minutes,
            override val minUpdateDistanceMeters: Int = 0,
            override val isPremiumFeature: Boolean = true
        ) : Balanced {
            override val key: String get() = Companion.key

            companion object {
                const val key: String = "balanced_level_500"
            }
        }

        companion object {
            val default: Balanced = Level2()
            fun fromKey(key: String?): Balanced {
                return when (key) {
                    Level1.key -> Level1()
                    Level2.key -> Level2()
                    Level3.key -> Level3()
                    Level4.key -> Level4()
                    Level5.key -> Level5()
                    else -> default
                }
            }
        }
    }
}