package app.paralino.core.provider

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource

actual fun StringResource.localizedString(): String {
    return CoreAppClass.instance.getString(this.resourceId)
}

actual fun StringResource.localizedString(vararg args: Any): String {
    return CoreAppClass.instance.getString(this.resourceId, *args)
}

actual fun PluralsResource.localizedString(quantity: Int): String {
    return CoreAppClass.instance.resources.getQuantityString(this.resourceId, quantity)
}

actual fun PluralsResource.localizedString(quantity: Int, vararg args: Any): String {
    return CoreAppClass.instance.resources.getQuantityString(this.resourceId, quantity, *args)
}