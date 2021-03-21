package com.zbapp.location.repo.location

import com.zbapp.location.models.location.BaseLocationModel

interface ILocationRemoteDataSource {

    suspend fun getAllLocations(): List<BaseLocationModel>
    suspend fun getLocationsForCircle(circleId: String): List<BaseLocationModel>
    suspend fun getLocationsForUser(userId: String): List<BaseLocationModel>
    suspend fun addLocations(locations: List<BaseLocationModel>)

}