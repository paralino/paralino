package app.paralino.core.provider

import dev.icerock.moko.resources.StringResource

actual fun StringResource.localizedString(): String {
    return CoreAppClass.instance.getString(this.resourceId)
}

actual fun StringResource.localizedString(vararg args: Any): String {
    return CoreAppClass.instance.getString(this.resourceId, *args)
}