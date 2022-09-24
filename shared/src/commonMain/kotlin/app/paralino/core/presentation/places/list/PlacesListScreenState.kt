package app.paralino.core.presentation.places.list

import app.paralino.core.models.base.ImageModel

data class PlacesListScreenState constructor(
    val groupId: String,
    val groupName: String,
    val places: List<PlaceListItemData>
)

data class PlaceListItemData constructor(
    val id: String,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Float,
    val lng: Float,
    val radiusMeters: Int,
)

sealed class PlacesScreenEvent() {
    data class Error(val error: Throwable) : PlacesScreenEvent()
}