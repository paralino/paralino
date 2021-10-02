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
                getGroupFromJson(it)
            }
        }
    }

    suspend fun getAllGroups(): List<SimpleDocumentRaw> {
        return blocking {
            api.getDocuments(Constants.collectionGroupUser).let {
                getGroupsFromJson(it)
            }
        }
    }

    suspend fun getGroup(groupId: String): SimpleDocumentRaw {
        return blocking {
            api.getDocument(Constants.collectionGroupUser, groupId).let {
                getGroupFromJson(it)
            }
        }
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            api.deleteDocument(Constants.collectionGroupUser, groupId)
        }
    }

    private fun getGroupFromJson(content: String): SimpleDocumentRaw {
        val json = BaseRemoteModule.formatter.parseToJsonElement(content)
        return SimpleDocumentRaw(
            id = json.jsonObject["id"]!!.jsonPrimitive.content,
            ownerId = null,
            data = json.jsonObject["data"]?.jsonObject?.jsonPrimitive?.content ?: ""
        )
    }

    private fun getGroupsFromJson(content: String): List<SimpleDocumentRaw> {
        val json = BaseRemoteModule.formatter.parseToJsonElement(content)
        return json.jsonObject["documents"]?.jsonArray?.map {
            SimpleDocumentRaw(
                id = it.jsonObject["id"]!!.jsonPrimitive.content,
                ownerId = null,
                data = it.jsonObject["data"]?.jsonObject?.jsonPrimitive?.content ?: ""
            )
        } ?: emptyList()
    }

}