package com.zbapp.core.repo.invite

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.invite.PersonalCodeData
import com.zbapp.core.models.raw.PersonalCodeCreateData
import com.zbapp.core.models.raw.PersonalCodeReadData
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.poll
import com.zbapp.core.utils.extensions.toJsonArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement

class InviteRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
) {
    suspend fun createInviteCode(data: PersonalCodeCreateData): PersonalCodeData {
        val id = functionsApi.createExecution(
            functionId = remoteConfig.resourceIds.functionPersonalCodeCreateID,
            data = BaseRemoteModule.formatter.encodeToString(data)
        )
        return poll {
            functionsApi.getExecution(remoteConfig.resourceIds.functionPersonalCodeCreateID, id)
                .takeIf { it.isSuccess }
                ?.let { getInviteCodes().firstOrNull { it.ownerId == data.ownerId } }
        }
    }

    suspend fun getInviteCodes(): List<PersonalCodeData> {
        val docs =
            docApi.getDocuments(remoteConfig.resourceIds.collectionPersonalCodeID).toJsonArray()
        return BaseRemoteModule.formatter.decodeFromJsonElement(docs)
    }

    suspend fun getInviteByCode(code: String): PersonalCodeReadData {
        val id = functionsApi.createExecution(
            functionId = remoteConfig.resourceIds.functionPersonalCodeReadID,
            code
        )
        return poll(canThrow = true) {
            // TOOD improve checking for valid code better
            functionsApi.getExecution(remoteConfig.resourceIds.functionPersonalCodeReadID, id)
                .takeIf { it.isSuccess }
                ?.let {
                    BaseRemoteModule.formatter.decodeFromString<PersonalCodeReadData>(it.stdout)
                }
        }
    }

    suspend fun revokeInviteCode(id: String) {
        docApi.deleteDocument(remoteConfig.resourceIds.collectionPersonalCodeID, id)
    }

}