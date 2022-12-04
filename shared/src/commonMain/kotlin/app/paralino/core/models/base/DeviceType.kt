package app.paralino.core.models.base

sealed interface DeviceType {
    val key: String

    object Android: DeviceType {
        override val key: String get() = "android"
    }

    object iOS: DeviceType {
        override val key: String get() = "ios"
    }
}
