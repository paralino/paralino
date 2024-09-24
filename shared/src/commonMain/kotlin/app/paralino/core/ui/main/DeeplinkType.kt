package app.paralino.core.ui.main

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.set

sealed class DeeplinkType() {
    abstract val deeplink: String

    open fun key(): String = deeplink

    data class ParalinoID(
        override val deeplink: String,
        val paralinoID: String
    ) : DeeplinkType() {

        companion object {
            val argumentParalinoID: String = "paralinoId"
            val deeplinkPrefix = "https://${host}/$pathParalinoID/"

            fun buildUrl(paralinoID: String): String {
                return "$deeplinkPrefix$paralinoID"
            }

            fun get(input: String): String {
                return (DeeplinkType.parse(input.lowercase()) as? ParalinoID)?.paralinoID ?: input
            }

            fun routeSignature(): String {
                return "$pathParalinoID/${argumentParalinoID}"
            }
        }
    }

    data class PasswordReset(
        override val deeplink: String,
        val userId: String,
        val secret: String,
        val expire: String?,
    ) : DeeplinkType() {

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

    data class EmailVerify(
        override val deeplink: String,
        val userId: String,
        val secret: String,
        val expire: String?,
    ) : DeeplinkType() {

        fun route(): String {
            return "$pathEmailVerify?$argumentUserId=$argumentUserId&$argumentSecret=$argumentSecret&$argumentExpire=$argumentExpire"
        }

        companion object {
            val argumentUserId: String = "userId"
            val argumentSecret: String = "secret"
            val argumentExpire: String = "expire"

            fun uriPattern(): String {
                return "https://${host}/${routeSignature()}"
            }

            fun routeSignature(): String {
                return "$pathEmailVerify?$argumentUserId={$argumentUserId}&$argumentSecret={$argumentSecret}&$argumentExpire={$argumentExpire}"
            }
        }
    }

    companion object {

        val host = "paralino.app"
        val linkHome = "home"
        val pathPasswordReset = "recover"
        val pathEmailVerify = "verify"
        val pathParalinoID = "id"

        private fun cleanDeepLink(deeplink: String?): Url? {

            deeplink ?: return null

            val data = Url(deeplink)
            val isValidDeeplink =
                data.host.endsWith(DeeplinkType.host) == true || (data.protocol.name == "paralino" && data.host == "app")

            if (!isValidDeeplink) return null

            val canUseDeeplink = arrayOf(
                DeeplinkType.pathPasswordReset,
                DeeplinkType.pathEmailVerify,
                DeeplinkType.pathParalinoID
            ).any {
                data.fullPath?.matches(it) ?: false
            }

            val cleanDeepLink = if (canUseDeeplink) {
                URLBuilder(data)
                    .apply { set(scheme = "https", host = DeeplinkType.host) }
                    .build()
            } else {
                Url("https://${DeeplinkType.host}/${DeeplinkType.linkHome}")
            }

            return cleanDeepLink
        }

        private fun String.matches(deeplinkRoute: String): Boolean {
            val currentComponents = this.split("/").filter { it.isNotBlank() }
            val otherComponents = deeplinkRoute.split("/").filter { it.isNotBlank() }
            if (currentComponents.firstOrNull() != otherComponents.firstOrNull()) return false

            return currentComponents.zip(otherComponents).all {
                if (it.second.startsWith("{")) it.first.isNotBlank()
                else it.first == it.second
            }
        }

        fun parse(deeplink: String?): DeeplinkType? {
            deeplink ?: return null
            val cleanDeeplink = cleanDeepLink(deeplink) ?: return null
            val path = cleanDeeplink.fullPath ?: return null
            val split = path.split("/").filter { it.isNotBlank() }

            return when {
                path.matches(pathPasswordReset) -> {
                    val userId =
                        cleanDeeplink.parameters.get(PasswordReset.argumentUserId) ?: return null
                    val secret =
                        cleanDeeplink.parameters.get(PasswordReset.argumentSecret) ?: return null
                    val expire = cleanDeeplink.parameters.get(PasswordReset.argumentExpire)

                    PasswordReset(deeplink, userId, secret, expire)
                }

                path.matches(pathEmailVerify) -> {
                    val userId =
                        cleanDeeplink.parameters.get(PasswordReset.argumentUserId) ?: return null
                    val secret =
                        cleanDeeplink.parameters.get(PasswordReset.argumentSecret) ?: return null
                    val expire = cleanDeeplink.parameters.get(PasswordReset.argumentExpire)

                    EmailVerify(deeplink, userId, secret, expire)
                }

                path.matches(pathParalinoID) -> {
                    val segments = cleanDeeplink.pathSegments ?: return null
                    val pathIndex =
                        segments.indexOfFirst { it == pathParalinoID }.takeIf { it >= 0 }
                            ?: return null
                    val paralinoId = segments.getOrNull(pathIndex + 1) ?: return null
                    ParalinoID(deeplink = deeplink, paralinoID = paralinoId)
                }

                else -> null
            }
        }
    }
    // "adb shell am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d https://paralino.app/recover?userId=SOME_ID&secret=SOME_SECRET&expire=1656958770"
}
