package app.paralino.core.provider

import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication
import platform.Foundation.NSURL

actual class ComposeUriHandler {
    actual fun openUri(uri: String) {
        UIApplication.sharedApplication.openURL(
            url = URLWithString(uri)!!,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }

    actual companion object {
        actual fun isUrlSchemeSupported(scheme: String): Boolean {
            val nsUrl = NSURL.URLWithString(scheme)
            return nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)
        }
    }
}