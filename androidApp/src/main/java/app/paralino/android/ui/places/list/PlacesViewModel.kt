package app.paralino.android.ui.places.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository

class PlacesViewModel(
    groupId: String,
    private val placesRepository: PlacesRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val coreVM = PlacesViewModel(groupId, placesRepository, groupRepository, viewModelScope)

    val uiState = coreVM.uiState
    val events = coreVM.events

}