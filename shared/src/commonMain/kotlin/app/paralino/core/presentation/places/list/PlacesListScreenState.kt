package app.paralino.core.presentation.places.list

import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.ConditionErrorType

data class PlacesListScreenState constructor(
    val groupId: String,
    val groupName: String,
    val places: List<PlaceListItemData>,
    val errors: List<ConditionErrorType>
)

data class PlaceListItemData constructor(
    val id: String,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
)

sealed class PlacesScreenEvent() {
    data class Error(val error: Throwable) : PlacesScreenEvent()
}