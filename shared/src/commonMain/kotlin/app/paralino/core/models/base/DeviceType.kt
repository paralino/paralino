package app.paralino.core.models.base

sealed interface DeviceType {
    val key: String

    data object Android: DeviceType {
        override val key: String get() = "android"
    }

    data object iOS: DeviceType {
        override val key: String get() = "ios"
    }
}
