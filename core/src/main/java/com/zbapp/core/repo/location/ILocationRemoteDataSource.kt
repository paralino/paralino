package com.zbapp.core.repo.location

import com.zbapp.core.models.location.BaseLocationModel

interface ILocationRemoteDataSource {

    suspend fun getAllLocations(): List<BaseLocationModel>
    suspend fun getLocationsForCircle(circleId: String): List<BaseLocationModel>
    suspend fun getLocationsForUser(userId: String): List<BaseLocationModel>
    suspend fun addLocations(locations: List<BaseLocationModel>)

}