package com.zbapp.core.repo.location

import com.zbapp.core.models.location.BaseLocationModel
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    fun getLocationsForGroup(groupId: String): Flow<List<BaseLocationModel>>
    fun getLocationsForUser(userId: String): Flow<List<BaseLocationModel>>
    suspend fun addLocation(locationModel: BaseLocationModel)
    suspend fun addLocations(locations: List<BaseLocationModel>)
    suspend fun clearAll()

}