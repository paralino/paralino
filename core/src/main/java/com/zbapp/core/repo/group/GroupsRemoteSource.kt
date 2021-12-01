package com.zbapp.core.repo.group

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.InvalidRequestException
import com.zbapp.core.utils.blocking
import kotlinx.serialization.decodeFromString

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val config: BackendConfig
) {

    suspend fun createOrUpdateGroup(group: GroupDocumentRaw): GroupDocumentRaw {
        return blocking {
            api.createOrUpdateDocument(config.collectionGroupsID, group.id, group.toPostJson()).let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    suspend fun getAllGroups(): List<GroupDocumentRaw> {
        return blocking {
            api.getDocuments(config.collectionGroupsID).let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    suspend fun getGroup(groupId: String): GroupDocumentRaw {
        return blocking {
            api.getDocument(config.collectionGroupsID, groupId).let {
                BaseRemoteModule.formatter.decodeFromString(it)
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