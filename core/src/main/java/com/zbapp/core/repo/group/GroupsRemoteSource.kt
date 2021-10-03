package com.zbapp.core.repo.group

import com.zbapp.core.config
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.utils.InvalidRequestException
import com.zbapp.core.utils.blocking

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi
) {

    suspend fun createOrUpdateGroup(group: SimpleDocumentRaw): SimpleDocumentRaw {
        return blocking {
            api.createOrUpdateDocument(config.collectionGroupsID, group).let {
                IDocumentsApi.getDocumentFromJson(it)
            }
        }
    }

    suspend fun getAllGroups(): List<SimpleDocumentRaw> {
        return blocking {
            api.getDocuments(config.collectionGroupsID).let {
                IDocumentsApi.getDocumentsFromJson(it)
            }
        }
    }

    suspend fun getGroup(groupId: String): SimpleDocumentRaw {
        return blocking {
            api.getDocument(config.collectionGroupsID, groupId).let {
                IDocumentsApi.getDocumentFromJson(it)
            }
        }
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            if(groupId.isBlank()) throw InvalidRequestException("Document id is invalid: $groupId")
            api.deleteDocument(config.collectionGroupsID, groupId)
        }
    }

}