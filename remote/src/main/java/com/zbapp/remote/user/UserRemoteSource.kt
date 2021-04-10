package com.zbapp.remote.user

import com.etebase.client.*
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserRemoteDataSource
import com.zbapp.remote.BaseRemoteModule
import com.zbapp.remote.Constants
import com.zbapp.remote.GroupsRemoteSource
import com.zbapp.remote.toUTCString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDateTime
import java.util.*

class UserRemoteSource(
    val serverUrl: String,
    val user: RemoteUserCache,
    val groupsRemote: GroupsRemoteSource
): IUserRemoteDataSource {

    private val cachedAccount get() = user.cachedAccount
    private val account: Account get() = user.account

    private fun getEtebaseClient(): Client {
        return BaseRemoteModule.getEtebaseClient(serverUrl)
    }

    override suspend fun signUp(email: String, password: String): String {
        val user = User(Utils.toBase64(Utils.randomBytes(24)), email)
        cachedAccount.value = Account.signup(getEtebaseClient(), user, password)

        initUserCollection(
            UserMe(
                id = user.username,
                email = email,
                userSince = LocalDateTime.now().toUTCString()
            )
        )

        groupsRemote.initGroupsCollection()

        return cachedAccount.value!!.save(null)
    }

    override suspend fun login(email: String, password: String): String {
        cachedAccount.value = Account.login(getEtebaseClient(), email, password)
        return cachedAccount.value!!.save(null)
    }

    override suspend fun restoreSession(accountData: String) {
        cachedAccount.value = Account.restore(getEtebaseClient(), accountData, null)
    }

    override suspend fun changePassword(newPassword: String) {
        account.changePassword(newPassword)
    }

    override suspend fun logout() {
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