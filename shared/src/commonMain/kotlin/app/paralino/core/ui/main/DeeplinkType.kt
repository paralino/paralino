package app.paralino.core.ui.main

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.set

sealed class DeeplinkType() {
    abstract val deeplink: String

    open fun key(): String = deeplink

    data class ParalinoID(
        override val deeplink: String,
        val paralinoID: String
    ) : DeeplinkType() {

        companion object {
            fun buildUrl(paralinoID: String): String {
                return "https://$host/$pathParalinoID/$paralinoID"
            }

            fun getParalinoIDCode(input: String): String {
                return (DeeplinkType.parse(input.lowercase()) as? ParalinoID)?.paralinoID ?: input
            }
        }
    }

    data class PasswordReset(
        override val deeplink: String,
        val userId: String,
        val secret: String,
        val expire: String?,
    ) : DeeplinkType() {
        companion object {
            const val argumentUserId: String = "userId"
            const val argumentSecret: String = "secret"
            const val argumentExpire: String = "expire"
        }
    }

    data class EmailVerify(
        override val deeplink: String,
        val userId: String,
        val secret: String,
        val expire: String?,
    ) : DeeplinkType() {
        companion object {
            const val argumentUserId: String = "userId"
            const val argumentSecret: String = "secret"
            const val argumentExpire: String = "expire"
        }
    }


    companion object {

        private const val host = "paralino.app"
        private const val linkHome = "home"
        private const val pathPasswordReset = "recover"
        private const val pathEmailVerify = "verify"
        private const val pathParalinoID = "id"

        private fun cleanDeepLink(deeplink: String?): Url? {

            deeplink ?: return null

            val data = try {
                Url(deeplink)
            } catch (e: Exception) {
                return null
            }
            val isValidDeeplink =
                data.host.endsWith(host) || (data.protocol.name == "paralino" && data.host == "app")

            if (!isValidDeeplink) return null

            val canUseDeeplink = arrayOf(
                pathPasswordReset,
                pathEmailVerify,
                pathParalinoID
            ).any {
                data.encodedPath.matches(it)
            }

            val cleanDeepLink = if (canUseDeeplink) {
                URLBuilder(data)
                    .apply { set(scheme = "https", host = DeeplinkType.host) }
                    .build()
            } else {
                Url("https://${host}/${linkHome}")
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
            val path = cleanDeeplink.encodedPath

            return when {
                path.matches(pathPasswordReset) -> {
                    val userId =
                        cleanDeeplink.parameters.get(PasswordReset.argumentUserId) ?: return null
                    val secret =
                        cleanDeeplink.parameters.get(PasswordReset.argumentSecret) ?: return null
                    val expire = cleanDeeplink.parameters.get(PasswordReset.argumentExpire)

                    PasswordReset(
                        deeplink = deeplink,
                        userId = userId,
                        secret = secret,
                        expire = expire
                    )
                }

                path.matches(pathEmailVerify) -> {
                    val userId =
                        cleanDeeplink.parameters.get(EmailVerify.argumentUserId) ?: return null
                    val secret =
                        cleanDeeplink.parameters.get(EmailVerify.argumentSecret) ?: return null
                    val expire = cleanDeeplink.parameters.get(EmailVerify.argumentExpire)

                    EmailVerify(
                        deeplink = deeplink,
                        userId = userId,
                        secret = secret,
                        expire = expire
                    )
                }

                path.matches(pathParalinoID) -> {
                    val segments = cleanDeeplink.rawSegments
                    val pathIndex = segments
                        .indexOfFirst { it == pathParalinoID }
                        .takeIf { it >= 0 }
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
