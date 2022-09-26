package app.paralino.android.ui.places.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository

class PlaceDetailViewModel(
    groupId: String,
    placeId: String,
    placeRepo: PlacesRepository,
    groupRepo: GroupRepository
) : ViewModel() {

    private val coreVm =
        PlaceDetailViewModel(groupId, placeId, placeRepo, groupRepo, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun deletePlace() = coreVm.deletePlace()
}