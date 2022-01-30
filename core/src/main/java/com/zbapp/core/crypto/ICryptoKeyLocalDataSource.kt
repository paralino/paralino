package com.zbapp.core.crypto

import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import kotlinx.coroutines.flow.Flow

interface ICryptoKeyLocalDataSource {

    suspend fun recreateAllGroupKeys(keys: List<GroupCryptoKeyDataRaw>)
    suspend fun recreateAllMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>)
    suspend fun recreateGroupKeyForGroup(groupId: String, key: GroupCryptoKeyDataRaw)
    suspend fun recreateMemberKeysForGroup(groupId: String, keys: List<GroupMemberCryptoKeyDataRaw>)

    suspend fun getGroupOwnerPublicKey(groupId: String): String?
    suspend fun getMemberPublicKey(groupId: String, userId: String): String?
    fun getAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>>
    fun getAllMemberKeys(): Flow<List<GroupMemberCryptoKeyDataRaw>>

    suspend fun deleteGroupKeyForGroup(groupId: String)
    suspend fun deleteAllKeysForGroup(groupId: String)
    suspend fun deleteKeysForUser(groupId: String, userId: String)

    suspend fun deleteAllKeys()
}