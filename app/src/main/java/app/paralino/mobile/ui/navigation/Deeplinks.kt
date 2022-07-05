package app.paralino.mobile.ui.navigation

import android.net.Uri
import app.paralino.mobile.ui.navigation.DeeplinkHandler.host
import app.paralino.mobile.ui.navigation.DeeplinkHandler.matches
import app.paralino.mobile.ui.navigation.DeeplinkHandler.pathPasswordReset

object DeeplinkHandler {

    val host = "paralino.app"
    private val linkHome = "home"
    private val linkPasswordReset = "recover"

    val pathPasswordReset = "$linkPasswordReset"

    fun cleanDeepLink(deeplink: String?): Uri? {

        deeplink ?: return null

        val data = Uri.parse(deeplink)
        val isValidDeeplink =
            data.host?.endsWith(host) == true || (data.scheme == "paralino" && data.host == "app")

        if (!isValidDeeplink) return null

        val canUseDeeplink = arrayOf(
            pathPasswordReset
        ).any {
            data.path?.matches(it) ?: false
        }

        val cleanDeepLink = if (canUseDeeplink) {
            data.buildUpon()
                .scheme("https")
                .authority(host)
                .build()
        } else {
            Uri.parse("https://$host/$linkHome")
        }

        return cleanDeepLink
    }

    fun String.matches(deeplinkRoute: String): Boolean {
        val currentComponents = this.split("/").filter { it.isNotBlank() }
        val otherComponents = deeplinkRoute.split("/").filter { it.isNotBlank() }
        if (currentComponents.size != otherComponents.size) return false

        return currentComponents.zip(otherComponents).all {
            if (it.second.startsWith("{")) it.first.isNotBlank()
            else it.first == it.second
        }
    }

    // "adb shell am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d https://paralino.app/recover?userId=SOME_ID&secret=SOME_SECRET&expire=1656958770"

}

sealed class Deeplink() {
    abstract val deeplink: String

    open fun key(): String = deeplink

    data class PasswordReset(
        override val deeplink: String,
        val userId: String,
        val secret: String,
        val expireUnix: Long?,
    ) : Deeplink() {

        fun route(): String {
            return "$pathPasswordReset?$argumentUserId=$argumentUserId&$argumentSecret=$argumentSecret&$argumentExpire=$argumentExpire"
        }

        companion object {
            val argumentUserId: String = "userId"
            val argumentSecret: String = "secret"
            val argumentExpire: String = "expire"

            fun uriPattern(): String {
                return "https://${host}/${routeSignature()}"
            }

            fun routeSignature(): String {
                return "$pathPasswordReset?$argumentUserId={$argumentUserId}&$argumentSecret={$argumentSecret}&$argumentExpire={$argumentExpire}"
            }
        }
    }

    companion object {
        fun parse(deeplink: String?): Deeplink? {
            deeplink ?: return null
            val cleanDeeplink = DeeplinkHandler.cleanDeepLink(deeplink) ?: return null
            val path = cleanDeeplink.path ?: return null
            val split = path.split("/").filter { it.isNotBlank() }

            return when {
                path.matches(pathPasswordReset) -> {
                    val userId =
                        cleanDeeplink.getQueryParameter(PasswordReset.argumentUserId) ?: return null
                    val secret =
                        cleanDeeplink.getQueryParameter(PasswordReset.argumentSecret) ?: return null
                    val expireUnix =
                        cleanDeeplink.getQueryParameter(PasswordReset.argumentExpire)?.toLong()

                    PasswordReset(deeplink, userId, secret, expireUnix)
                }
                else -> null
            }
        }
    }

}