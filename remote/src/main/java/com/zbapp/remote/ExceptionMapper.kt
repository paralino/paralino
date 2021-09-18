package com.zbapp.remote

import com.zbapp.core.utils.BaseRemoteException
import io.appwrite.exceptions.AppwriteException

object ExceptionMapper {
    fun map(exception: Exception): Exception {
        val sourceException = (exception as? AppwriteException) ?: return exception
        return BaseRemoteException.fromCode(sourceException.code, sourceException.message)
    }
}