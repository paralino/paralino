package com.zbapp.remote.api

import com.etebase.client.*
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserRemoteDataSource
import com.zbapp.core.utils.NotFoundException
import com.zbapp.remote.*
import com.zbapp.remote.user.RemoteUserCache
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDateTime
import java.util.*

internal class UserApi constructor(
    private val user: RemoteUserCache,
    private val groupsRemote: GroupsRemoteSource
): IUserRemoteDataSource {

    private val cachedAccount get() = user.accountCache
    private val account: Account get() = user.account
    private lateinit var client: Client

    private suspend fun initServer(serverUrl: String) {
        client = BaseRemoteModule.getEtebaseClient(serverUrl)
    }

    private fun getEtebaseClient(): Client {
        return client
    }

    override suspend fun generateUsername(): String {
        return Utils.toBase64(Utils.randomBytes(16))
    }

    override suspend fun signUp(serverUrl: String, username: String, email: String, password: String): String {
        initServer(serverUrl)
        val user = User(username, email)
        cachedAccount.value = blocking { Account.signup(getEtebaseClient(), user, password) }
        return cachedAccount.value!!.save(null)
    }

    override suspend fun login(serverUrl: String, email: String, password: String): String {
        initServer(serverUrl)
        cachedAccount.value = blocking { Account.login(getEtebaseClient(), email, password) }
        return cachedAccount.value!!.save(null)
    }

    override suspend fun restoreSession(serverUrl: String, accountData: String) {
        initServer(serverUrl)
        cachedAccount.value = blocking { Account.restore(getEtebaseClient(), accountData, null) }
    }

    override suspend fun changePassword(newPassword: String) {
        blocking { account.changePassword(newPassword) }
    }

    override suspend fun logout() {
        blocking { cachedAccount.value?.logout() }
        cachedAccount.value = null
    }

    override suspend fun getMe(): UserMe {
        val collection = blocking {
            account.collectionManager.list(Constants.collectionTypeUserProfile).data.firstOrNull()
        } ?: throw NotFoundException("User not found")
        return BaseRemoteModule.formatter.decodeFromString<UserMe>(collection.contentString)
    }

    override suspend fun updateName(name: String) {
        updateUser { it.copy(name = name) }
    }

    override suspend fun updateImage(imageBase64: String) {
        updateUser { it.copy(imageBase64 = imageBase64) }
    }

    private suspend fun updateUser(modify: (UserMe) -> UserMe) {
        blocking {
            val collection =
                account.collectionManager.list(Constants.collectionTypeUserProfile).data.first()
            val user = BaseRemoteModule.formatter.decodeFromString<UserMe>(collection.contentString)
            collection.content =
                BaseRemoteModule.formatter.encodeToString(modify(user)).toByteArray()
            collection.meta.mtime = Date().time
            account.collectionManager.upload(collection)
        }
    }

    override suspend fun initVerifiedUser(user: UserMe): UserMe {
        val newUser = user.copy(
            userSince = LocalDateTime.now().toUTCString()
        )

        initUserCollection(newUser)
        groupsRemote.initGroups()

        return newUser
    }

    private suspend fun initUserCollection(user: UserMe) {
        blocking {
            val meta = ItemMetadata()
            meta.name = "User settings"
            val collection = account.collectionManager.create(
                Constants.collectionTypeUserProfile,
                meta,
                BaseRemoteModule.formatter.encodeToString(user)
            )
            account.collectionManager.upload(collection)
        }
    }


}