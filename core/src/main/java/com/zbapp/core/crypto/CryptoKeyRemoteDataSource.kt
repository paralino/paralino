package com.zbapp.core.crypto

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.blocking
import com.zbapp.core.utils.toJsonObject
import kotlinx.serialization.json.encodeToJsonElement

class CryptoKeyRemoteDataSource(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoAuth: CryptoAuth
) {

    suspend fun getAllGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupKeysID).map { it.parseGroupKey() }
        }
    }

    suspend fun getAllGroupKeysForGroup(groupId: String): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupKeysID, "groupId" to groupId).map { it.parseGroupKey() }
        }
    }

    suspend fun getAllGroupMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersKeysID).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun getAllGroupMemberKeysForGroup(groupId: String): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersKeysID, "groupId" to groupId).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun recreateMemberKeysForGroup(groupId: String, keys: List<GroupMemberCryptoKeyDataRaw>) {
        blocking {
            val payload = mapOf(
                "groupId" to groupId,
                "keys" to keys.map { BaseRemoteModule.formatter.encodeToJsonElement(it) }
            ).toJsonObject().toString()
            functionsApi.runAndWait(remoteConfig.resourceIds.functionCreateGroupID, payload)
        }
    }

    suspend fun recreateGroupKeysForGroup(groupId: String, keys: List<GroupCryptoKeyDataRaw>) {
        blocking {
            val payload = mapOf(
                "groupId" to groupId,
                "keys" to keys.map { BaseRemoteModule.formatter.encodeToJsonElement(it) }
            ).toJsonObject().toString()
            functionsApi.runAndWait(remoteConfig.resourceIds.functionCreateGroupID, payload)
        }
    }

    private suspend fun Map<String, Any>.parseGroupKey(): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw.from(this)
    }

    private suspend fun Map<String, Any>.parseGroupMemberKey(): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw.from(this)
    }

}