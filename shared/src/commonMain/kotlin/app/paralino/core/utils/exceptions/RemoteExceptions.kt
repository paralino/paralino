package app.paralino.core.utils.exceptions

import app.paralino.core.appPrefs
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.raw.FunctionErrorBody
import app.paralino.core.provider.NetworkConnectionInfo

open class BaseRemoteException constructor(
    message: String?,
    cause: Throwable?,
    open val code: Int? = null,
    open val type: String? = null
) : Exception(message, cause) {
    companion object {

        const val typeUserInvalidToken = "user_invalid_token"

        fun fromRemote(
            code: Int?,
            error: FunctionErrorBody?,
        ): BaseRemoteException {
            return fromCode(
                code = code,
                message = error?.message,
                cause = null,
                type = error?.type
            )
        }

        fun fromCode(
            code: Int?,
            message: String?,
            cause: Throwable?,
            type: String? = null,
        ): BaseRemoteException {
            return when (code) {
                400 -> InvalidRequestException(message, cause)
                401 -> UnauthorizedException(code, type, message, cause)
                403, 402 -> {
                    if(!appPrefs.emailConfirmed) UnverifiedUserException(code, message, cause)
                    else PermissionDeniedException(code, message, cause)
                }
                404 -> NotFoundException(type = type, message = message, cause = cause)
                409 -> ConflictException(message, cause)
                429, 503, 504 -> RemoteConnectionException(message, code, cause)
                else -> GenericServerException(
                    message = message,
                    code = code,
                    type = type,
                    cause = cause
                )
            }
        }
    }

    override fun toString(): String {
        return super.toString() + " code: $code, type: $type"
    }
}

class InternalConnectionException(val status: NetworkConnectionInfo, message: String? = null): BaseRemoteException(message, null, null)
class RemoteConnectionException(message: String?, code: Int?, cause: Throwable? = null): BaseRemoteException(message, cause, code)
class UnverifiedUserException(code: Int?, message: String?, cause: Throwable? = null): UnauthorizedException(code, null, message, cause)
class PermissionDeniedException(code: Int?, message: String?, cause: Throwable? = null): UnauthorizedException(code, null, message, cause)
open class UnauthorizedException(code: Int?, type: String?, message: String?, cause: Throwable? = null): BaseRemoteException(
    code = code,
    message = message,
    cause = cause,
    type = type
) {
    val isUserBlocked: Boolean = type == UserSessionMonitor.userBlocked
}

class InvalidRequestException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)

open class NotFoundException(message: String?, type: String? = null, cause: Throwable? = null): BaseRemoteException(
    type = type,
    message = message,
    cause = cause,
    code = 404
)
class ConflictException(message: String?, cause: Throwable? = null): BaseRemoteException(
    message = message,
    cause = cause,
    code = 409
)

class GenericServerException(
    override val message: String?,
    override val code: Int?,
    override val type: String?,
    cause: Throwable? = null
) : BaseRemoteException(message = message, cause = cause, code = code, type = type)

class TimeoutException(message: String?, cause: Throwable? = null) : BaseRemoteException(message, cause)
class DataStillLoadingException(message: String?, cause: Throwable? = null) : BaseRemoteException(message, cause)

fun Throwable.asRemoteException() = this as? BaseRemoteException?