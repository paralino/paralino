package app.paralino.android.ui.places.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.models.base.ImageModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.presentation.places.create.CreatePlaceViewModel

class CreatePlaceViewModel(
    private val groupId: String,
    private val placeId: String?,
    private val groupRepo: GroupRepository,
    private val placeRepo: PlacesRepository
) : ViewModel() {
    private val coreVm =
        CreatePlaceViewModel(groupId, placeId, placeRepo, groupRepo, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun createPlace() = coreVm.createPlace()
    fun updateName(name: String) = coreVm.updateName(name)
    fun updateImage(image: ImageModel) = coreVm.updateImage(image)
    fun updatePosition(lat: Double, lng: Double) = coreVm.updatePosition(lat, lng)
    fun updateRadius(radiusMeters: Int) = coreVm.updateRadius(radiusMeters)

}