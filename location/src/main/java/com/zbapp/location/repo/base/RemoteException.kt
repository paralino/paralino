package com.zbapp.location.repo.base

class RemoteException(
    val url: String?,
    val code: Int = 0,
    message: String?,
    exception: Throwable?
): RuntimeException("$message (for) $url", exception) {

    val isNPE = cause is KotlinNullPointerException
    val isHTTP = code > 0

}

fun Throwable.asRemoteException() = this as? RemoteException?