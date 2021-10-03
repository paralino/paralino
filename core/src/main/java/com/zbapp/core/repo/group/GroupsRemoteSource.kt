package com.zbapp.core.repo.group

import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.Constants
import com.zbapp.core.utils.blocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi
) {

    suspend fun createOrUpdateGroup(group: SimpleDocumentRaw): SimpleDocumentRaw {
        return blocking {
            api.createOrUpdateDocument(Constants.collectionGroupUser, group).let {
                IDocumentsApi.getDocumentFromJson(it)
            }
        }
    }

    suspend fun getAllGroups(): List<SimpleDocumentRaw> {
        return blocking {
            api.getDocuments(Constants.collectionGroupUser).let {
                IDocumentsApi.getDocumentsFromJson(it)
            }
        }
    }

    suspend fun getGroup(groupId: String): SimpleDocumentRaw {
        return blocking {
            api.getDocument(Constants.collectionGroupUser, groupId).let {
                IDocumentsApi.getDocumentFromJson(it)
            }
        }
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            api.deleteDocument(Constants.collectionGroupUser, groupId)
        }
    }

}