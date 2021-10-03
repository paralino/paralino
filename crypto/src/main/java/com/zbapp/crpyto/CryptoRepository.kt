package com.zbapp.crpyto

import android.app.Application
import com.zbapp.core.repo.group.ICryptoRepository
import java.util.*

class CryptoRepository(private val app: Application) : ICryptoRepository {

    var cryptoAuth: CryptoAuth? = null
    var cryptoGroups: CryptoGroups? = null

    override fun init(userId: String) {
        cryptoAuth = CryptoAuth(app, userId)
        cryptoGroups = CryptoGroups(cryptoAuth!!)
    }

    override fun reset() {
        cryptoAuth = null
        cryptoGroups = null
    }

    override suspend fun encryptData(groupId: String, data: String): String {
        return cryptoGroups!!.encryptData(groupId, data)
    }

    override suspend fun decryptData(groupId: String, data: String, senderId: String?, dataDate: Date?): String {
        return cryptoGroups!!.decryptData(groupId, data, senderId, dataDate)
    }

    override suspend fun encryptData(data: String): String {
        return cryptoAuth!!.encryptData(data)
    }

    override suspend fun decryptData(data: String): String {
        return cryptoAuth!!.decryptData(data)
    }
}