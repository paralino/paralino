package app.paralino.core.presentation.places.create

import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.Emojis

data class CreatePlaceScreenState constructor(
    val groupId: String,
    val isNewPlace: Boolean,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
    val hasNameError: Boolean,
    val actionLoading: Boolean,
    val fullLoading: Boolean
) {

    val currentStep: Int get() = (radiusMeters / minRadiusMeters)
    companion object {

        const val minRadiusMeters = 50
        const val defaultRadiusMeters = 200
        const val steps = 61

        fun empty(groupId: String): CreatePlaceScreenState {
            return CreatePlaceScreenState(
                groupId = groupId,
                isNewPlace = true,
                name = "",
                image = ImageModel.emoji(Emojis.group1.random()),
                private = false,
                lat = null,
                lng = null,
                radiusMeters = defaultRadiusMeters,
                hasNameError = false,
                actionLoading = false,
                fullLoading = false
            )
        }
    }

}

sealed class CreatePlaceScreenEvent() {
    data class Error(val error: Throwable): CreatePlaceScreenEvent()
    object MissingInfo: CreatePlaceScreenEvent()
    object MissingLocation: CreatePlaceScreenEvent()
    object PlaceUpdated: CreatePlaceScreenEvent()
}