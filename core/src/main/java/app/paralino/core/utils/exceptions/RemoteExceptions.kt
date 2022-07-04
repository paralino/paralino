package app.paralino.core.utils.exceptions

import app.paralino.core.config
import app.paralino.core.utils.ConnectionStatus

open class BaseRemoteException constructor(
    message: String?,
    cause: Throwable?,
    val code: Int? = null,
    val type: String? = null
) : Exception(message, cause) {
    companion object {
        fun fromCode(
            code: Int?,
            message: String?,
            cause: Throwable?,
            type: String? = null,
        ): BaseRemoteException {
            return when (code) {
                400 -> InvalidRequestException(message, cause)
                401 -> UnauthorizedException(type, message, cause)
                403 -> {
                    if(!config.emailConfirmed) UnverifiedUserException(message, cause)
                    else PermissionDeniedException(message, cause)
                }
                404 -> NotFoundException(message, cause)
                409 -> ConflictException(message, cause)
                429, 503, 504 -> RemoteConnectionException(message, code, cause)
                else -> GenericServerException(message, code, cause)
            }
        }
    }
}

class InternalConnectionException(val status: ConnectionStatus, message: String? = null): BaseRemoteException(message, null, null)
class RemoteConnectionException(message: String?, code: Int?, cause: Throwable? = null): BaseRemoteException(message, cause, code)
class UnverifiedUserException(message: String?, cause: Throwable? = null): UnauthorizedException(null, message, cause)
class PermissionDeniedException(message: String?, cause: Throwable? = null): UnauthorizedException(null, message, cause)
open class UnauthorizedException(type: String?, message: String?, cause: Throwable? = null): BaseRemoteException(message, cause, type = type)
class InvalidRequestException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
open class NotFoundException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class ConflictException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class GenericServerException(message: String?, code: Int?, cause: Throwable? = null) : BaseRemoteException(message, cause, code)
class TimeoutException(message: String?, cause: Throwable? = null) : BaseRemoteException(message, cause)

fun Throwable.asRemoteException() = this as? BaseRemoteException?