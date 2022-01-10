package com.zbapp.core.repo.location

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.LocationHistoryRaw
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.utils.blocking

class LocationRemoteSource constructor(
    private val api: IDocumentsApi,
    private val config: BackendConfig
) {

    suspend fun getCurrentLocationsForGroup(groupId: String): List<MemberDocumentRaw> {
        return blocking {
            api.getDocuments(config.collectionLocationsID).map {
                MemberDocumentRaw.from(it)
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
            api.createOrUpdateDocument(
                collectionId = config.collectionLocationsID,
                docId = data.groupId,
                data = data.toPostJson(),
                read = readPermissions
            ).let { MemberDocumentRaw.from(it) }
        }
    }

}