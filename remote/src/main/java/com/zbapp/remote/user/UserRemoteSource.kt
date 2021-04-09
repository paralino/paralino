package com.zbapp.remote.user

import com.etebase.client.*
import com.zbapp.core.config
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserRemoteDataSource
import com.zbapp.remote.BaseRemoteModule
import com.zbapp.remote.Constants
import com.zbapp.remote.toUTCString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.*

class UserRemoteSource(val serverUrl: String): IUserRemoteDataSource {

    var cachedAccount = MutableStateFlow<Account?>(null)
    val account: Account get() = cachedAccount.value!!

    private fun getEtebaseClient(): Client {
        return BaseRemoteModule.getEtebaseClient(serverUrl)
    }

    suspend fun signUp(email: String, password: String) {
        val user = User(Utils.toBase64(Utils.randomBytes(24)), email)
        cachedAccount.value = Account.signup(getEtebaseClient(), user, password)

        initUserCollection(
            UserMe(
                id = user.username,
                email = email,
                userSince = LocalDateTime.now().toUTCString()
            )
        )

        config.token = cachedAccount.value?.save(null) ?: ""
    }

    suspend fun login(email: String, password: String) {
        cachedAccount.value = Account.login(getEtebaseClient(), email, password)
        config.token = cachedAccount.value?.save(null) ?: ""
    }

    suspend fun restoreSession(accountData: String) {
        cachedAccount.value = Account.restore(getEtebaseClient(), accountData, null)
    }

    override suspend fun changePassword(newPassword: String) {
        account.changePassword(newPassword)
    }

    suspend fun logout() {
        config.token = ""
        cachedAccount.value?.logout()
        cachedAccount.value = null
    }

    override suspend fun getMe(): UserMe {
        val collection = account.collectionManager.list(Constants.collectionTypeUserProfile).data.first()
        return BaseRemoteModule.formatter.decodeFromString<UserMe>(collection.contentString)
    }

    override suspend fun updateName(name: String) {
        updateUser { it.copy(name = name) }
    }

    override suspend fun updateImage(imageBase64: String) {
        updateUser { it.copy(imageBase64 = imageBase64) }
    }

    private suspend fun updateUser(modify: (UserMe) -> UserMe) {
        val collection = account.collectionManager.list(Constants.collectionTypeUserProfile).data.first()
        val user = BaseRemoteModule.formatter.decodeFromString<UserMe>(collection.contentString)
        collection.content = BaseRemoteModule.formatter.encodeToString(modify(user)).toByteArray()
        collection.meta.mtime = Date().time
        account.collectionManager.upload(collection)
    }

    private suspend fun initUserCollection(user: UserMe) {
        val meta = ItemMetadata()
        meta.name = "User settings"
        account.collectionManager.create(
            Constants.collectionTypeUserProfile,
            meta,
            BaseRemoteModule.formatter.encodeToString(user)
        )
    }


}