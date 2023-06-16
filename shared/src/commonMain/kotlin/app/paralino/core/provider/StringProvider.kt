package app.paralino.core.provider

import dev.icerock.moko.resources.StringResource

expect fun StringResource.localizedString(): String
expect fun StringResource.localizedString(vararg args: Any): String