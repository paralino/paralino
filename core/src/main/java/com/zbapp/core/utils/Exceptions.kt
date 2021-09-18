package com.zbapp.core.utils

import com.zbapp.core.config

open class BaseRemoteException(message: String?): Exception(message) {
    companion object {
        fun fromCode(code: Int?, message: String?): BaseRemoteException {
            return when(code) {
                500 -> ConnectionException(message)
                401 -> UnauthorizedException(message)
                403 -> {
                    if(!config.emailConfirmed) UnverifiedUserException(message)
                    else PermissionDeniedException(message)
                }
                404 -> NotFoundException(message)
                304 -> ConflictException(message)
                301 -> {
                    if(message?.contains("unique_email") == true)
                        ConflictException(message)
                    else if(message?.contains("invalid_mail_server") == true)
                        NotFoundException(message)
                    else
                        ConnectionException(message)
                }
                else -> GenericServerException(message)
            }
        }
    }
}

class UnverifiedUserException(message: String?): BaseRemoteException(message)
class PermissionDeniedException(message: String?): BaseRemoteException(message)
class UnauthorizedException(message: String?): BaseRemoteException(message)
class ConnectionException(message: String?): BaseRemoteException(message)
class NotFoundException(message: String?): BaseRemoteException(message)
class ConflictException(message: String?): BaseRemoteException(message)
class GenericServerException(message: String?) : BaseRemoteException(message)

