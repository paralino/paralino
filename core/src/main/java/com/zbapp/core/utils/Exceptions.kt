package com.zbapp.core.utils

open class BaseRemoteException(message: String?): Exception(message)

class UnverifiedUserException(message: String?): BaseRemoteException(message)
class PermissionDeniedException(message: String?): BaseRemoteException(message)
class UnauthorizedException(message: String?): BaseRemoteException(message)
class ConnectionException(message: String?): BaseRemoteException(message)
class NotFoundException(message: String?): BaseRemoteException(message)
class ConflictException(message: String?): BaseRemoteException(message)
class GenericServerException(message: String?) : BaseRemoteException(message)
