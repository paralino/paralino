package com.zbapp.location.repo.location

import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.location.repo.LocationSharingManager
import com.zbapp.location.utils.replace
import kotlinx.coroutines.flow.*

class LocationRepository constructor(
    val sharingManager: LocationSharingManager
) {

    // map<circleId, listOfLocations>
    private val locations = MutableStateFlow(emptyMap<String, List<BaseLocationModel>>())

    fun getCurrentSharedLocations(): Flow<List<BaseLocationModel>> {
        return locations.map { it.flatMap { it.value } }
    }

    fun getLocationsForCircle(circleId: String): Flow<List<BaseLocationModel>> {
        return locations.map {
            it.getOrDefault(circleId, emptyList())
        }
    }

    fun getLocationsForUser(userId: String): Flow<List<BaseLocationModel>> {
        return locations.map {
            it.flatMap { it.value }.filter { it.userId == userId }
        }
    }

    fun getLastLocationForUser(userId: String): Flow<BaseLocationModel?> {
        return getLocationsForUser(userId).map {
            it.minByOrNull { it.time ?: 0 }
        }
    }

    fun pushLocation(location: BaseLocationModel): Flow<Unit> {
        locations.replace {
            toMutableMap().apply {
                sharingManager.allContractsValue.forEach {
                    val list = this.getOrDefault(it.circleId, emptyList())
                        .toMutableList().apply {
                            val index = this.indexOfFirst { it.userId == location.userId }
                            if(index < 0) {
                                this.add(location)
                            } else {
                                this[index] = location
                            }
                        }
                    this[it.circleId] = list
                }
            }
        }
        return flow { emit(Unit) }
    }


}