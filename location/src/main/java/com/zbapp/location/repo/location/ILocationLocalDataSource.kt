package com.zbapp.location.repo.location

import com.zbapp.location.models.location.BaseLocationModel
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    fun getLocationsForCircle(circleId: String): Flow<List<BaseLocationModel>>
    fun getLocationsForUser(userId: String): Flow<List<BaseLocationModel>>
    suspend fun addLocation(locationModel: BaseLocationModel)
    suspend fun addLocations(locations: List<BaseLocationModel>)
    suspend fun clearAll()

}