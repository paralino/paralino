package com.zbapp.core.repo.location

import com.zbapp.core.models.raw.SimpleDocumentRaw
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    suspend fun addLocation(data: SimpleDocumentRaw)
    suspend fun addLocations(data: List<SimpleDocumentRaw>)

    fun getLocationForUser(userId: String): Flow<SimpleDocumentRaw>
    fun getLocationsForUsers(userIds: List<String>): Flow<List<SimpleDocumentRaw>>

    suspend fun clearForUser(userId: String)
    suspend fun clearAll()

}