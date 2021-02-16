package com.zbapp.location.repo.location

import com.zbapp.location.models.circle.Circle
import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.location.models.location.LocationSharingContract
import kotlinx.coroutines.flow.*

class LocationRepository {

    private val locations = MutableStateFlow(emptyMap<String, List<BaseLocationModel>>())

    private val personalTracking = LocationSharingContract(Circle.personalTracking.id, null)

    private val _publicSharing = LocationSharingContract(Circle.publicSharing.id, null)
    // only consider public sharing if duration is explicitly set
    private val publicSharing get() = _publicSharing.takeIf { it.endTimeStamp != null }

    private val userContracts = MutableStateFlow(emptyList<LocationSharingContract>())
    private val allContracts get() = userContracts.map {
        it + listOfNotNull(personalTracking, publicSharing)
    }

    fun getCurrentSharedLocations(): Flow<List<BaseLocationModel>> {
        return locations.map { it.flatMap { it.value } }
    }

    fun getLocationsFor(circle: Circle): Flow<List<BaseLocationModel>> {
        return locations.map {
            it.getOrDefault(circle.id, emptyList())
        }
    }

    fun getLocationsFor(userId: String): Flow<List<BaseLocationModel>> {
        return locations.map {
            it.flatMap { it.value }.filter { it.userId == userId }
        }
    }

    fun getLastLocationForUser(userId: String): Flow<BaseLocationModel?> {
        return getLocationsFor(userId).map {
            it.minByOrNull { it.time ?: 0 }
        }
    }

    fun pushLocation(location: BaseLocationModel): Flow<Unit> {
        allContracts.map {
            it.forEach {
                locations.value = locations.value.toMutableMap().apply {
                    val list = this.getOrDefault(it.circleId, emptyList())
                    list.toMutableList().apply {
                        add(location)
                        distinctBy { it.userId }
                    }
                    this[it.circleId] = list
                }
            }
        }
        return flow { emit(Unit) }
    }

    fun enableSharing(contract: LocationSharingContract) {
        userContracts.value = userContracts.value.toMutableList().apply {
            removeAll { it.circleId == contract.circleId }
            add(contract)
        }
    }

    fun stopSharing(circleId: String) {
        userContracts.value = userContracts.value.toMutableList().apply {
            removeAll { it.circleId == circleId }
        }
    }

}