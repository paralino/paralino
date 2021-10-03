package com.zbapp.core.repo.group

import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.*

interface ICryptoRepository: IUserCrypto, IGroupCrypto {
    fun init(userId: String)
    fun reset()
}

interface IGroupCrypto {
    suspend fun encryptData(groupId: String, data: String): String
    suspend fun decryptData(groupId: String, data: String, senderId: String? = null, dataDate: Date? = null): String
}

interface IUserCrypto {
    suspend fun encryptData(data: String): String
    suspend fun decryptData(data: String): String
}

suspend inline fun <reified T> IUserCrypto.encryptModel(data: T): String {
    return encryptData(BaseRemoteModule.formatter.encodeToString(data))
}

suspend inline fun <reified T> IUserCrypto.decryptModel(data: String): T {
    return BaseRemoteModule.formatter.decodeFromString(decryptData(data))
}


suspend inline fun <reified T> IGroupCrypto.encryptModel(groupId: String, data: T): String {
    return encryptData(groupId, BaseRemoteModule.formatter.encodeToString(data))
}

suspend inline fun <reified T> IGroupCrypto.decryptModel(groupId: String, data: String, senderId: String? = null, dataDate: Date? = null): T {
    return BaseRemoteModule.formatter.decodeFromString(decryptData(groupId, data, senderId, dataDate))
}