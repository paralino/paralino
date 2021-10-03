package com.zbapp.crpyto

import com.virgilsecurity.android.common.model.FindUsersResult
import com.virgilsecurity.android.ethreeenclave.interaction.EThree
import com.virgilsecurity.sdk.cards.Card
import com.zbapp.core.repo.group.IGroupCrypto
import java.util.*

class CryptoGroups(
    private val cryptoAuth: CryptoAuth
): IGroupCrypto {

    private val eThree: EThree = cryptoAuth.ethree
    private val groups = hashMapOf<String, CryptoGroupData>()
    private val users = FindUsersResult()

    suspend fun init(groupIds: List<GroupDescriptor>, userIds: List<String>) {
        reloadUsers(userIds, false)
        reloadAllGroups(groupIds)
    }

    suspend fun reloadAllGroups(groupIds: List<GroupDescriptor>) {
        blocking {
            groupIds.forEach { group ->
                reloadGroup(group.groupId, group.ownerId)
            }
        }
    }

    private suspend fun reloadUsers(userIds: List<String>, checkResult: Boolean): FindUsersResult {
        return blocking {
            eThree.findUsers(userIds, forceReload = true, checkResult = checkResult).get().also {
                it.forEach { t, u ->
                    users[t] = u
                }
            }
        }
    }

    suspend fun createGroup(groupId: String, userIds: List<String> = emptyList()) {
        blocking {
            eThree.createGroup(groupId, getUsersCard(userIds)).get().also {
                groups[groupId] = CryptoGroupData(it)
            }
        }
    }

    suspend fun updateParticipants(groupId: String, newUserList: List<String>) {
        blocking {
            val group = getGroupLocally(groupId)

            if(group == null) {
                // create new group with provided users
                createGroup(groupId, newUserList)
                return@blocking
            }

            val currentUserList = group.getParticipantsIds()
            if(newUserList.isEmpty()) {
                // remove all users
                group.removeUsers(getUsersCard(currentUserList)!!)
                return@blocking
            }

            val toRemove = currentUserList.subtract(newUserList).distinct()
            val toAdd = newUserList.subtract(currentUserList).distinct()

            if(toRemove.isNotEmpty()) {
                group.removeUsers(getUsersCard(toRemove)!!)
            }

            if(toAdd.isNotEmpty()) {
                group.addUsers(getUsersCard(toAdd)!!)
            }

        }
    }

    override suspend fun encryptData(groupId: String, data: String): String {
        return getGroupLocally(groupId)?.encryptData(data)!!
    }

    override suspend fun decryptData(groupId: String, data: String, senderId: String?, dataDate: Date?): String {
        return blocking {
            val group = getGroupLocally(groupId)
            val sender = senderId ?: group!!.ownerId
            group!!.decryptData(getUserCard(sender)!!, data, dataDate)
        }
    }

    suspend fun reloadGroup(groupId: String, ownerId: String): CryptoGroupData {
        return blocking {
            reloadGroup(groupId, getUserCard(ownerId)!!)
        }
    }

    suspend fun reloadGroup(groupId: String, ownerCard: Card): CryptoGroupData {
        return blocking {
            eThree.loadGroup(groupId, ownerCard).get().let {
                CryptoGroupData(it).also {
                    groups[groupId] = it
                }
            }
        }
    }

    suspend fun tryReloadGroup(groupId: String) {
        blocking {
            getGroupLocally(groupId)?.let {
                reloadGroup(groupId, it.ownerId)
            }
        }
    }

    private fun getGroupLocally(groupId: String): CryptoGroupData? {
        val groupInMemory = groups.get(groupId)
        if(groupInMemory != null) return groupInMemory

        val localGroup = eThree.getGroup(groupId)
        if(localGroup != null) {
            return CryptoGroupData(localGroup).also {
                groups[groupId] = it
            }
        }

        return null
    }

    private suspend fun getUsersCard(userIds: List<String>): FindUsersResult? {

        // in memory
        if(userIds.size == users.size && users.all { userIds.contains(it.value.identity) }){
            return FindUsersResult(users.filter { userIds.contains(it.value.identity) })
        }

        return try {
            reloadUsers(userIds, true)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getUserCard(userId: String): Card? {
        val userInMemory = users.get(userId)
        if(userInMemory != null) return userInMemory

        val localUser = eThree.findCachedUser(userId).get()
        if(localUser != null) {
            users[userId] = localUser
            return localUser
        }

        val remoteUser = blocking {
            try {
                eThree.findUser(userId).get()
            } catch (e: Exception) {
                null
            }
        }

        if(remoteUser != null) {
            users[userId] = remoteUser
            return remoteUser
        }

        return null
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            eThree.deleteGroup(groupId).execute()
            groups.remove(groupId)
        }
    }

}

data class GroupDescriptor(
    val groupId: String,
    val ownerId: String
)