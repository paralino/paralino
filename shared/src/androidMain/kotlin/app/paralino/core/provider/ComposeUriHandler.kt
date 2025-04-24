package app.paralino.core.provider

import app.paralino.core.utils.extensions.openUri

actual class ComposeUriHandler {
    actual fun openUri(uri: String) {
        CoreAppClass.instance.applicationContext.openUri(uri)
    }
}