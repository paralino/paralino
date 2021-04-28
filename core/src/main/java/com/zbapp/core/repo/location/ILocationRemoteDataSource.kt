package com.zbapp.core.repo.location

import com.zbapp.core.models.location.BaseLocationModel

interface ILocationRemoteDataSource {

    suspend fun getCurrentLocationsForGroup(groupId: String): List<BaseLocationModel>
    suspend fun getLocationsHistoryForUser(groupId: String, userId: String): List<BaseLocationModel>
    suspend fun getMyLocationsHistory(date: String): List<BaseLocationModel>
    suspend fun setLocations(current: List<BaseLocationModel>, history: List<BaseLocationModel> = emptyList())

}