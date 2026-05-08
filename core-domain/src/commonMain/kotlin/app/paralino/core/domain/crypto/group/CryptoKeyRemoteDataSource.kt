package app.paralino.core.domain.crypto.group

import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.extensions.blocking

class CryptoKeyRemoteDataSource constructor(
    private val api: IDocumentsApi,
    private val appConfig: AppConfigRepository,
) {

    suspend fun getAllGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionGroupKeysID
            ).map { it.parseGroupKey() }
        }
    }

    suspend fun getGroupKeyForGroup(groupId: String): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionGroupKeysID,
                queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
            ).map { it.parseGroupKey() }
        }
    }

    suspend fun getAllGroupMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionGroupMembersKeysID
            ).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun getAllGroupMemberKeysForGroup(groupId: String): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionGroupMembersKeysID,
                queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
            ).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun getAllGroupDeviceKeys(): List<GroupDeviceCryptoKeyRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionGroupDeviceKeysID
            ).map { it.parseGroupDeviceKey() }
        }
    }

    suspend fun getAllGroupDeviceKeysForGroup(groupId: String): List<GroupDeviceCryptoKeyRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionGroupDeviceKeysID,
                queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
            ).map { it.parseGroupDeviceKey() }
        }
    }

    suspend fun getAllPersonalGroupKeys(): List<PersonalGroupKeyRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionPersonalGroupKeysID
            ).map { it.parseGPersonalGroupKey() }
        }
    }

    suspend fun getAllPersonalGroupKeysForGroup(groupId: String): List<PersonalGroupKeyRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionPersonalGroupKeysID,
                queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
            ).map { it.parseGPersonalGroupKey() }
        }
    }

    private fun Map<String, Any>.parseGroupKey(): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw.from(this)
    }

    private fun Map<String, Any>.parseGroupMemberKey(): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw.from(this)
    }

    private fun Map<String, Any>.parseGroupDeviceKey(): GroupDeviceCryptoKeyRaw {
        return GroupDeviceCryptoKeyRaw.from(this)
    }

    private fun Map<String, Any>.parseGPersonalGroupKey(): PersonalGroupKeyRaw {
        return PersonalGroupKeyRaw.from(this)
    }

}