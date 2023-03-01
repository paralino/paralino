package app.paralino.core.presentation.places.list

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.presentation.groups.GroupDetailScreenEvent
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis

data class PlacesListScreenState constructor(
    val groupId: String,
    val groupName: String,
    val placesLimit: Int,
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
) {
    constructor(place: GroupPlace): this(
        id = place.id,
        name = place.data.data?.name ?: "---",
        image = place.data.data?.image ?: ImageModel.emoji(Emojis.robot),
        private = place.private,
        lat = place.data.data?.lat,
        lng = place.data.data?.lng,
        radiusMeters = place.data.data?.radiusMeters ?: 0
    )
}

sealed class PlacesScreenEvent() {
    data class Error(val error: Throwable) : PlacesScreenEvent()
    object AddPlace : PlacesScreenEvent()
    data class GroupPlacesLimitReached(val limit: Int) : PlacesScreenEvent()
}