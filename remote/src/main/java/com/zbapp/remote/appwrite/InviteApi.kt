package com.zbapp.remote.appwrite

import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.repo.group.IDocumentsApi
import com.zbapp.core.repo.invite.IInviteApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.TimeoutException
import com.zbapp.core.utils.randomString
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class InviteApi constructor(
    private val remoteModule: AppWriteRemoteModule,
    private val documentsApi: IDocumentsApi
) : IInviteApi {
    override suspend fun createInviteCode(data: InviteCode): InviteCode {
        return remoteModule.execute {
            val tempId = randomString()
            val payload = BaseRemoteModule.formatter.encodeToString(data.copy(tempId = tempId))
            functions.createExecution(
                functionId = remoteModule.config.functionInviteCreateID,
                data = payload
            )
            poll { getInviteCodes().firstOrNull { it.tempId == tempId } }
        }
    }

    override suspend fun getInviteCodes(): List<InviteCode> {
        val docs = documentsApi.getDocuments(remoteModule.config.collectionInvitesID)
        val json = BaseRemoteModule.formatter.parseToJsonElement(docs)
        return json.jsonObject["documents"]?.jsonArray?.map {
            BaseRemoteModule.formatter.decodeFromJsonElement(it)
        } ?: emptyList()
    }

    override suspend fun getInviteByCode(code: String): InviteCode {
        return remoteModule.execute {
//            functions.createExecution(
//                functionId = remoteModule.config.functionInviteGetCodeID,
//                code
//            )
            poll { getInviteCodes().firstOrNull { it.matchesInviteCode(code) } }
        }
    }

    override suspend fun acceptInviteByCode(code: String): InviteCode {
        return remoteModule.execute {
            functions.createExecution(
                functionId = remoteModule.config.functionInviteAcceptID,
                code
            )
            poll { getInviteCodes().firstOrNull { it.matchesInviteCode(code) } }
        }
    }

    override suspend fun getInviteByUrl(url: String): InviteCode {
        return remoteModule.execute {
            functions.createExecution(functionId = config.functionInviteGetUrlID, url)
            poll { getInviteCodes().firstOrNull { it.inviteUrl == url } }
        }
    }

    private suspend fun poll(action: suspend () -> InviteCode?): InviteCode {
        return withTimeoutOrNull(15_000) {
            delay(1_000)
            var invite: InviteCode? = action()
            while (invite == null) {
                delay(3_000)
                invite = action()
            }
            invite
        } ?: throw TimeoutException("Invite code request timed out")
    }

    override suspend fun revokeInviteCode(id: String) {
        documentsApi.deleteDocument(remoteModule.config.collectionInvitesID, id)
    }

}