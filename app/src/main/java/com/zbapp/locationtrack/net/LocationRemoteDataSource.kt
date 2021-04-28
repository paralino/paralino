package com.zbapp.locationtrack.net

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.location.ILocationRemoteDataSource
import kotlinx.coroutines.flow.MutableStateFlow

class LocationRemoteDataSource: ILocationRemoteDataSource {

    // temp local data
    private val locations = MutableStateFlow(emptyMap<String, List<BaseLocationModel>>())

    override suspend fun getCurrentLocationsForGroup(groupId: String): List<BaseLocationModel> {
        return locations.value.getOrDefault(groupId, emptyList())
    }

    override suspend fun getLocationsHistoryForUser(groupId: String, userId: String): List<BaseLocationModel> {
        return locations.value.flatMap { it.value }.filter { it.userId == userId }
    }

    override suspend fun getMyLocationsHistory(date: String): List<BaseLocationModel> {
        return emptyList()
    }

    override suspend fun setLocations(
        current: List<BaseLocationModel>,
        history: List<BaseLocationModel>
    ) {
        val list = this.locations.value.toMutableMap().also { map ->
            current.forEach { location ->
                val list = map.getOrDefault(location.groupId, emptyList())
                    .toMutableList().apply {
                        val index = this.indexOfFirst { it.userId == location.userId }
                        if (index < 0) {
                            this.add(location)
                        } else {
                            this[index] = location
                        }
                    }
                map[location.groupId] = list
            }
        }

        this.locations.value = list
    }

}