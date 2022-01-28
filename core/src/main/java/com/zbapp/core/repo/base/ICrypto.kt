package com.zbapp.core.repo.base

import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

interface IUserCrypto {
    suspend fun encryptData(data: String): String
    suspend fun decryptData(data: String): String
}

suspend inline fun <reified T> IUserCrypto.encryptModel(data: T?): String? {
    data ?: return null
    return encryptData(BaseRemoteModule.formatter.encodeToString(data))
}

suspend inline fun <reified T> IUserCrypto.decryptModel(data: String?): T? {
    data ?: return null
    return BaseRemoteModule.formatter.decodeFromString(decryptData(data))
}