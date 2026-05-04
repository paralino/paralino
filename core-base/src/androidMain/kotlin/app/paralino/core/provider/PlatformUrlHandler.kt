package app.paralino.core.provider

import android.content.Context
import app.paralino.core.utils.extensions.openUri

class PlatformUrlHandler(val applicationContext: Context): IPlatformUrlHandler {
    override fun openUrl(url: String) {
        applicationContext.openUri(url)
    }
}