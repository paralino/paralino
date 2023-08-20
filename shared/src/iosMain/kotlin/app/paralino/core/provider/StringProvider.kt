package app.paralino.core.provider

import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import dev.icerock.moko.resources.format

actual fun StringResource.localizedString(): String {
    return this.desc().localized()
}

actual fun StringResource.localizedString(vararg args: Any): String {
    return this.format(*args).localized()
}