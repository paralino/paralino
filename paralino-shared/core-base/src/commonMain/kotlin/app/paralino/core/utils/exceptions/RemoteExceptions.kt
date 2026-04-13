package app.paralino.core.utils.exceptions

import app.paralino.core.provider.NetworkConnectionInfo

open class BaseRemoteException constructor(
    message: String?,
    cause: Throwable?,
    open val code: Int? = null,
    open val type: String? = null
) : Exception(message, cause) {
    companion object {

        const val typeUserInvalidToken = "user_invalid_token"

        fun fromCode(
            code: Int?,
            message: String?,
            cause: Throwable?,
            type: String? = null,
        ): BaseRemoteException {
            return when (code) {
                400 -> InvalidRequestException(
                    code = code,
                    type = type,
                    message = message,
                    cause = cause
                )
                401 -> UnauthorizedException(code, type, message, cause)
                403, 402 -> {
                    PermissionDeniedException(
                        code = code,
                        message = message,
                        type = type,
                        cause = cause
                    )
                }
                404 -> NotFoundException(type = type, message = message, cause = cause)
                409 -> ConflictException(
                    code = code,
                    type = type,
                    message = message,
                    cause = cause
                )
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
class PermissionDeniedException(code: Int?, message: String?, type: String? = null, cause: Throwable? = null): UnauthorizedException(
    code = code,
    type = type,
    message = message,
    cause = cause
)
open class UnauthorizedException(code: Int?, type: String?, message: String?, cause: Throwable? = null): BaseRemoteException(
    code = code,
    message = message,
    cause = cause,
    type = type
)

class InvalidRequestException(
    code: Int?,
    type: String?,
    message: String?,
    cause: Throwable? = null
): BaseRemoteException(
    message = message,
    cause = cause,
    code = code,
    type = type
)

open class NotFoundException(message: String?, type: String? = null, cause: Throwable? = null): BaseRemoteException(
    type = type,
    message = message,
    cause = cause,
    code = 404
)
class ConflictException(
    code: Int?,
    type: String?,
    message: String?,
    cause: Throwable? = null
): BaseRemoteException(
    message = message,
    cause = cause,
    code = code,
    type = type
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