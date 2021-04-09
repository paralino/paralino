package com.zbapp.core.repo.location

import com.zbapp.core.models.location.BaseLocationModel

interface ILocationRemoteDataSource {

    suspend fun getCurrentLocationsForCircle(circleId: String): List<BaseLocationModel>
    suspend fun getLocationsHistoryForUser(circleId: String, userId: String): List<BaseLocationModel>
    suspend fun getMyLocationsHistory(date: String): List<BaseLocationModel>
    suspend fun setLocations(current: List<BaseLocationModel>, history: List<BaseLocationModel>)

}