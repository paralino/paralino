package com.zbapp.core.repo.location

import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.models.raw.LocationHistoryRaw
import com.zbapp.core.repo.group.IDocumentsApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.Constants
import com.zbapp.core.utils.blocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LocationRemoteSource constructor(
    private val api: IDocumentsApi
) {

    suspend fun getCurrentLocationsForGroup(groupId: String): List<SimpleDocumentRaw> {
        return blocking {
            api.getDocuments(Constants.collectionGroupItemCurrent).let {
                getGroupsFromJson(it)
            }
        }
    }

    suspend fun getLocationsHistoryForUser(groupId: String, userId: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun getMyLocationsHistory(date: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun saveCurrentLocation(data: BaseLocationModel) {
        blocking {
            val content = BaseRemoteModule.formatter.encodeToString(data)
            val data = SimpleDocumentRaw(data.id, content)
            api.createOrUpdateDocument(Constants.collectionGroupItemCurrent, data)
        }
    }

    suspend fun saveLocationHistory(history: List<BaseLocationModel>) {
        TODO("Not yet implemented")
    }

    private fun getGroupFromJson(content: String): SimpleDocumentRaw {
        val json = BaseRemoteModule.formatter.parseToJsonElement(content)
        return SimpleDocumentRaw(
            id = json.jsonObject["id"]!!.jsonPrimitive.content,
            data = json.jsonObject["data"]?.jsonObject?.jsonPrimitive?.content ?: ""
        )
    }

    private fun getGroupsFromJson(content: String): List<SimpleDocumentRaw> {
        val json = BaseRemoteModule.formatter.parseToJsonElement(content)
        return json.jsonObject["documents"]?.jsonArray?.map {
            SimpleDocumentRaw(
                id = it.jsonObject["id"]!!.jsonPrimitive.content,
                data = it.jsonObject["data"]?.jsonObject?.jsonPrimitive?.content ?: ""
            )
        } ?: emptyList()
    }

}