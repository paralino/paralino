package com.zbapp.core.repo.group

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.utils.InvalidRequestException
import com.zbapp.core.utils.blocking

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val config: BackendConfig
) {

    suspend fun createOrUpdateGroup(group: GroupDocumentRaw): GroupDocumentRaw {
        return blocking {
            api.createOrUpdateDocument(config.collectionGroupsID, group).let {
                GroupDocumentRaw.fromRemote(IDocumentsApi.getDocumentFromJson(it))
            }
        }
    }

    suspend fun getAllGroups(): List<GroupDocumentRaw> {
        return blocking {
            api.getDocuments(config.collectionGroupsID).let {
                IDocumentsApi.getDocumentsFromJson(it).map {
                    GroupDocumentRaw.fromRemote(it)
                }
            }
        }
    }

    suspend fun getGroup(groupId: String): GroupDocumentRaw {
        return blocking {
            api.getDocument(config.collectionGroupsID, groupId).let {
                GroupDocumentRaw.fromRemote(IDocumentsApi.getDocumentFromJson(it))
            }
        }
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            if (groupId.isBlank()) throw InvalidRequestException("Document id is invalid: $groupId")
            api.deleteDocument(config.collectionGroupsID, groupId)
        }
    }

}