package com.zbapp.core.repo.location

import com.zbapp.core.models.raw.LocationHistoryRaw
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.repo.group.IDocumentsApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.Constants
import com.zbapp.core.utils.blocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LocationRemoteSource constructor(
    private val api: IDocumentsApi
) {

    suspend fun getCurrentLocationsForGroup(groupId: String): List<SimpleDocumentRaw> {
        return blocking {
            api.getDocuments(Constants.collectionGroupItemCurrent).let {
                IDocumentsApi.getDocumentsFromJson(it)
            }
        }
    }

    suspend fun getLocationsHistoryForUser(groupId: String, userId: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun getMyLocationsHistory(date: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun saveCurrentLocation(data: SimpleDocumentRaw, readPermissions: List<String>): SimpleDocumentRaw {
        return blocking {
            val response = api.createOrUpdateDocument(Constants.collectionGroupItemCurrent, data, readPermissions)
            IDocumentsApi.getDocumentFromJson(response)
        }
    }

}