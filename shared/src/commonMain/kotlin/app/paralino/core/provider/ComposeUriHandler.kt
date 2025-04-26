package app.paralino.core.provider

import androidx.compose.runtime.staticCompositionLocalOf

val ComposeLocalUriHandler = staticCompositionLocalOf<ComposeUriHandler> {
    error("CompositionLocal ComposeUriHandler not present")
}

expect class ComposeUriHandler constructor(){
    /**
     * Open given URL in browser
     *
     * @throws IllegalArgumentException when given [uri] is invalid and/or can't be handled by the
     * system
     */
    fun openUri(uri: String)

    companion object {
        fun isUrlSchemeSupported(scheme: String) : Boolean
    }
}