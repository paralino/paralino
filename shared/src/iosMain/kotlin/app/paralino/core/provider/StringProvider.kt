package app.paralino.core.provider

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import dev.icerock.moko.resources.format

actual fun StringResource.localizedString(): String {
    return this.desc().localized()
}

actual fun StringResource.localizedString(vararg args: Any): String {
    return this.format(*args).localized()
}

actual fun PluralsResource.localizedString(quantity: Int): String {
    return this.desc(quantity).localized()
}

actual fun PluralsResource.localizedString(quantity: Int, vararg args: Any): String {
    return this.format(quantity, *args).localized()
}