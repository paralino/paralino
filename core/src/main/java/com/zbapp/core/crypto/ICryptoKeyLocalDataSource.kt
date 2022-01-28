package com.zbapp.core.crypto

import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import kotlinx.coroutines.flow.Flow

interface ICryptoKeyLocalDataSource {

    suspend fun recreateAllGroupKeys(keys: List<GroupCryptoKeyDataRaw>)
    suspend fun recreateAllMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>)
    suspend fun recreateGroupKeyForGroup(groupId: String, key: GroupCryptoKeyDataRaw)
    suspend fun recreateMemberKeysForGroup(groupId: String, keys: List<GroupMemberCryptoKeyDataRaw>)

    fun getAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>>
    fun getAllMemberKeys(): Flow<List<GroupMemberCryptoKeyDataRaw>>

    fun getGroupKey(groupId: String): Flow<GroupCryptoKeyDataRaw>
    suspend fun getGroupKeyOnce(groupId: String): GroupCryptoKeyDataRaw
    fun getMemberKey(groupId: String, memberId: String): Flow<GroupMemberCryptoKeyDataRaw>
    suspend fun getMemberKeyOnce(groupId: String, memberId: String): GroupMemberCryptoKeyDataRaw

    suspend fun deleteGroupKeyForGroup(groupId: String)
    suspend fun deleteAllKeysForGroup(groupId: String)
    suspend fun deleteKeysForUser(groupId: String, userId: String)

    suspend fun deleteAllKeys()
}