package com.zbapp.core.repo.invite

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.poll
import com.zbapp.core.utils.randomString
import com.zbapp.core.utils.toJsonArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement

class InviteRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
) {
    suspend fun createInviteCode(data: InviteCode): InviteCode {
        val tempId = randomString()
        val payload = BaseRemoteModule.formatter.encodeToString(data.copy(tempId = tempId))
        functionsApi.createExecution(
            functionId = remoteConfig.functionInviteCreateID,
            data = payload
        )
        return poll { getInviteCodes().firstOrNull { it.tempId == tempId } }
    }

    suspend fun getInviteCodes(): List<InviteCode> {
        val docs = docApi.getDocuments(remoteConfig.collectionInvitesID).toJsonArray()
        return BaseRemoteModule.formatter.decodeFromJsonElement(docs)
    }

    suspend fun getInviteByCode(code: String): InviteCode {
        functionsApi.createExecution(
            functionId = remoteConfig.functionInviteGetCodeID,
            code
        )
        return poll { getInviteCodes().firstOrNull { it.matchesInviteCode(code) } }
    }

    suspend fun acceptInviteByCode(code: String): InviteCode {
        functionsApi.createExecution(
            functionId = remoteConfig.functionInviteAcceptID,
            code
        )
        return poll { getInviteCodes().firstOrNull { it.matchesInviteCode(code) } }
    }

    suspend fun getInviteByUrl(url: String): InviteCode {
        functionsApi.createExecution(functionId = remoteConfig.functionInviteGetUrlID, url)
        return poll { getInviteCodes().firstOrNull { it.inviteUrl == url } }
    }

    suspend fun revokeInviteCode(id: String) {
        docApi.deleteDocument(remoteConfig.collectionInvitesID, id)
    }

}