package com.zbapp.crpyto

import android.app.Application
import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.crypto.ICryptoAuthProvider
import com.zbapp.core.repo.base.ICryptoRepository
import java.util.*

class CryptoRepository(
    private val app: Application,
    private val cryptoAuthProvider: ICryptoAuthProvider
) : ICryptoRepository {

    var cryptoAuth: CryptoAuth? = null
    var cryptoGroups: CryptoGroups? = null

    override fun reset() {
        cryptoAuth?.logout()
        cryptoAuth = null
        cryptoGroups = null
    }

    override suspend fun encryptData(groupId: String, data: String): String {
        return cryptoGroups!!.encryptData(groupId, data)
    }

    override suspend fun decryptData(
        groupId: String,
        data: String,
        senderId: String?,
        dataDate: Date?
    ): String {
        return cryptoGroups!!.decryptData(groupId, data, senderId, dataDate)
    }

    override suspend fun encryptData(data: String): String {
        return cryptoAuth!!.encryptData(data)
    }

    override suspend fun decryptData(data: String): String {
        return cryptoAuth!!.decryptData(data)
    }
}

class DummyCryptoRepository constructor(
    private val cryptoAuth: CryptoAuth
) : ICryptoRepository {

    override fun reset() {
        cryptoAuth.logout()
    }

    override suspend fun encryptData(data: String): String {
        return cryptoAuth!!.encryptData(data)
    }

    override suspend fun encryptData(groupId: String, data: String): String {
        return data
    }

    override suspend fun decryptData(data: String): String {
        return cryptoAuth!!.decryptData(data)
    }

    override suspend fun decryptData(
        groupId: String,
        data: String,
        senderId: String?,
        dataDate: Date?
    ): String {
        return data
    }

}