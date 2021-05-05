package com.zbapp.remote

import com.etebase.client.exceptions.*
import com.zbapp.core.config
import com.zbapp.core.utils.GenericServerException
import com.zbapp.core.utils.UnverifiedUserException

object ExceptionMapper {

    fun map(exception: Exception): Exception {
        val etebaseException = (exception as? EtebaseException) ?: return exception
        val message = etebaseException.message
        return when(etebaseException) {
            is ConnectionException -> com.zbapp.core.utils.ConnectionException(message)
            is UnauthorizedException -> com.zbapp.core.utils.UnauthorizedException(message)
            is PermissionDeniedException -> {
                if(!config.emailConfirmed) UnverifiedUserException(message)
                else com.zbapp.core.utils.PermissionDeniedException(message)
            }
            is NotFoundException -> com.zbapp.core.utils.NotFoundException(message)
            is ConflictException -> com.zbapp.core.utils.ConflictException(message)
            is HttpException -> {
                if(message?.contains("unique_email") == true)
                    com.zbapp.core.utils.ConflictException(message)
                if(message?.contains("invalid_mail_server") == true)
                    com.zbapp.core.utils.NotFoundException(message)
                else
                    com.zbapp.core.utils.ConnectionException(message)
            }
            else -> GenericServerException(message)
        }
    }

}