package com.zbapp.locationtrack.net

import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.location.repo.location.ILocationRemoteDataSource
import kotlinx.coroutines.flow.MutableStateFlow

class LocationRemoteDataSource: ILocationRemoteDataSource {

    // temp local data
    private val locations = MutableStateFlow(emptyMap<String, List<BaseLocationModel>>())

    override suspend fun getAllLocations(): List<BaseLocationModel> {
        return locations.value.flatMap { it.value }
    }

    override suspend fun getLocationsForCircle(circleId: String): List<BaseLocationModel> {
        return locations.value.getOrDefault(circleId, emptyList())
    }

    override suspend fun getLocationsForUser(userId: String): List<BaseLocationModel> {
        return locations.value.flatMap { it.value }.filter { it.userId == userId }
    }

    override suspend fun addLocations(locations: List<BaseLocationModel>) {

        val list = this.locations.value.toMutableMap().also { map ->
            locations.forEach { location ->
                val list = map.getOrDefault(location.circleId, emptyList())
                    .toMutableList().apply {
                        val index = this.indexOfFirst { it.userId == location.userId }
                        if (index < 0) {
                            this.add(location)
                        } else {
                            this[index] = location
                        }
                    }
                map[location.circleId] = list
            }
        }

        this.locations.value = list
    }

}