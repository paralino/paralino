package app.paralino.android.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.location.LocationRefreshUseCase
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository


class MapViewModel constructor(
    app: Application,
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val locationRefreshUseCase: LocationRefreshUseCase
) : AndroidViewModel(app) {

    private val coreVM = MapHomeViewModel(locationRepo, sharingManager, groupRepo, placesRepo, locationRefreshUseCase, viewModelScope)

    val uiState = coreVM.uiState
    val locationRefreshSubscription = coreVM.locationRefreshSubscription

    fun switchGroup(groupId: String) = coreVM.switchGroup(groupId)
    fun trackMarker(markerId: String?) = coreVM.trackMarker(markerId)
    suspend fun getAllGroups() = coreVM.getAllGroups()
    fun clearMyLocation() = coreVM.clearMyLocation()

}