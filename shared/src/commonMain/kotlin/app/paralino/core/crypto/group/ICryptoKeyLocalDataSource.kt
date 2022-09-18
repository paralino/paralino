package app.paralino.core.crypto.group

import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import kotlinx.coroutines.flow.Flow

interface ICryptoKeyLocalDataSource {

    suspend fun recreateAllGroupKeys(groupKeys: List<GroupCryptoKeyDataRaw>, memberKeys: List<GroupMemberCryptoKeyDataRaw>)
    suspend fun recreateAllGroupKeys(keys: List<GroupCryptoKeyDataRaw>)
    suspend fun recreateAllMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>)
    suspend fun recreateGroupKeyForGroup(groupId: String, groupKeys: List<GroupCryptoKeyDataRaw>, memberKeys: List<GroupMemberCryptoKeyDataRaw>)
    suspend fun recreateGroupKeyForGroup(groupId: String, keys: List<GroupCryptoKeyDataRaw>)
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