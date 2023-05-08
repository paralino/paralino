package app.paralino.core.crypto.group

import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw
import app.paralino.core.models.raw.PersonalPreKeyRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.extensions.blocking

class CryptoKeyRemoteDataSource constructor(
    private val api: IDocumentsApi,
    private val remoteConfig: BackendConfig,
) {

    suspend fun getAllGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupKeysID
            ).map { it.parseGroupKey() }
        }
    }

    suspend fun getGroupKeyForGroup(groupId: String): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupKeysID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parseGroupKey() }
        }
    }

    suspend fun getAllGroupMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersKeysID
            ).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun getAllGroupMemberKeysForGroup(groupId: String): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersKeysID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun getAllPersonalGroupKeys(): List<PersonalGroupKeyRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionPersonalGroupKeysID
            ).map { it.parseGPersonalGroupKey() }
        }
    }

    suspend fun getAllPersonalGroupKeysForGroup(groupId: String): List<PersonalGroupKeyRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionPersonalGroupKeysID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parseGPersonalGroupKey() }
        }
    }

    private fun Map<String, Any>.parseGroupKey(): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw.from(this)
    }

    private fun Map<String, Any>.parseGroupMemberKey(): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw.from(this)
    }

    private fun Map<String, Any>.parseGroupPreKey(): PersonalPreKeyRaw {
        return PersonalPreKeyRaw.from(this)
    }

    private fun Map<String, Any>.parseGPersonalGroupKey(): PersonalGroupKeyRaw {
        return PersonalGroupKeyRaw.from(this)
    }

}