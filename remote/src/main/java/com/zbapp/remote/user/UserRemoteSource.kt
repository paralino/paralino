package com.zbapp.remote.user

import com.etebase.client.*
import com.zbapp.core.config
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserRemoteDataSource
import com.zbapp.remote.*
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
    private val client = BaseRemoteModule.getEtebaseClient(serverUrl)

    private fun getEtebaseClient(): Client {
        return client
    }

    override suspend fun generateUsername(): String {
        return Utils.toBase64(Utils.randomBytes(16))
    }

    override suspend fun signUp(username: String, email: String, password: String): String {
        val user = User(username, email)
        cachedAccount.value = blocking { Account.signup(getEtebaseClient(), user, password) }
        return cachedAccount.value!!.save(null)
    }

    override suspend fun login(email: String, password: String): String {
        cachedAccount.value = blocking { Account.login(getEtebaseClient(), email, password) }
        return cachedAccount.value!!.save(null)
    }

    override suspend fun restoreSession(accountData: String) {
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
        val collection = blocking { account.collectionManager.list(Constants.collectionTypeUserProfile).data.first() }
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

    suspend fun initCollections() {
        initUserCollection(
            UserMe(
                id = config.userId,
                email = config.email,
                userSince = LocalDateTime.now().toUTCString()
            )
        )

        groupsRemote.initGroupsCollection()
    }

    private suspend fun initUserCollection(user: UserMe) {
        blocking {
            val meta = ItemMetadata()
            meta.name = "User settings"
            account.collectionManager.create(
                Constants.collectionTypeUserProfile,
                meta,
                BaseRemoteModule.formatter.encodeToString(user)
            )
        }
    }


}