package com.zbapp.core.utils

import com.zbapp.core.config

open class BaseRemoteException(
    message: String?,
    cause: Throwable?,
    val code: Int? = null
) : Exception(message, cause) {
    companion object {
        fun fromCode(code: Int?, message: String?, cause: Throwable?): BaseRemoteException {
            return when (code) {
                400 -> InvalidRequestException(message, cause)
                401 -> UnauthorizedException(message, cause)
                403 -> {
                    if(!config.emailConfirmed) UnverifiedUserException(message, cause)
                    else PermissionDeniedException(message, cause)
                }
                404 -> NotFoundException(message, cause)
                409 -> ConflictException(message, cause)
                429, 503, 504 -> ConnectionException(message, code, cause)
                else -> GenericServerException(message, code, cause)
            }
        }
    }
}

class ConnectionException(message: String?, code: Int?, cause: Throwable? = null): BaseRemoteException(message, cause, code)
class UnverifiedUserException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class PermissionDeniedException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class UnauthorizedException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class InvalidRequestException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class NotFoundException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class ConflictException(message: String?, cause: Throwable? = null): BaseRemoteException(message, cause)
class GenericServerException(message: String?, code: Int?, cause: Throwable? = null) : BaseRemoteException(message, cause, code)

