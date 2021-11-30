package com.zbapp.core.repo.location

import com.zbapp.core.models.raw.MemberDocumentRaw
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    suspend fun addLocation(data: MemberDocumentRaw)
    suspend fun addLocations(data: List<MemberDocumentRaw>)

    fun getLocationForUser(userId: String): Flow<MemberDocumentRaw>
    fun getLocationsForUsers(userIds: List<String>): Flow<List<MemberDocumentRaw>>

    suspend fun clearForUser(userId: String)
    suspend fun clearAll()

}