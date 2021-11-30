package com.zbapp.core.repo.location

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.LocationHistoryRaw
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.repo.group.IDocumentsApi
import com.zbapp.core.utils.blocking

class LocationRemoteSource constructor(
    private val api: IDocumentsApi,
    private val config: BackendConfig
) {

    suspend fun getCurrentLocationsForGroup(groupId: String): List<MemberDocumentRaw> {
        return blocking {
            api.getDocuments(config.collectionLocationsID).let {
                IDocumentsApi.getDocumentsFromJson(it).map {
                    MemberDocumentRaw.fromRemote(it)
                }
            }
        }
    }

    suspend fun getLocationsHistoryForUser(groupId: String, userId: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun getMyLocationsHistory(date: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun saveCurrentLocation(
        data: MemberDocumentRaw,
        readPermissions: List<String>
    ): MemberDocumentRaw {
        return blocking {
            val response =
                api.createOrUpdateDocument(config.collectionLocationsID, data, readPermissions)
            MemberDocumentRaw.fromRemote(IDocumentsApi.getDocumentFromJson(response))
        }
    }

}