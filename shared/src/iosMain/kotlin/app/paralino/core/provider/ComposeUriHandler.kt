package app.paralino.core.provider

import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication

actual class ComposeUriHandler {
    actual fun openUri(uri: String) {
        UIApplication.sharedApplication.openURL(
            url = URLWithString(uri)!!,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }
}