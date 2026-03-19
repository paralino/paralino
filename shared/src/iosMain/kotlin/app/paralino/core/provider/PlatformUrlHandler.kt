package app.paralino.core.provider

import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication

class PlatformUrlHandler: IPlatformUrlHandler {
    override fun openUrl(url: String) {
        UIApplication.sharedApplication.openURL(
            url = URLWithString(url)!!,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }
}