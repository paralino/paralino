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

    val currentStep: Int get() = steps.indexOfFirst{ radiusMeters <= it }.minus(1).takeIf { it >= 0 } ?: 0
    companion object {
        
        const val defaultRadiusMeters = 200
        val steps = arrayOf(50, 80, 100, 150, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1200, 1500, 1800, 2000, 2200, 2500, 2800, 3000)

        fun radiusForIndex(index: Int): Int {
            return steps.getOrNull(index-1) ?: defaultRadiusMeters
        }

        fun empty(groupId: String): CreatePlaceScreenState {
            return CreatePlaceScreenState(
                groupId = groupId,
                isNewPlace = true,
                name = "",
                image = ImageModel.emoji(Emojis.groupSpecific.random()),
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