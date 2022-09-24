package app.paralino.core.presentation.places

import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.Emojis

data class CreatePlaceScreenState(
    val isNewPlace: Boolean,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Float,
    val lng: Float,
    val radiusMeters: Int,
    val hasNameError: Boolean,
    val isLoading: Boolean
) {

    companion object {
        fun empty(): CreatePlaceScreenState {
            return CreatePlaceScreenState(
                isNewPlace = true,
                name = "",
                image = ImageModel.emoji(Emojis.group1.random()),
                private = false,
                lat = 0f,
                lng = 0f,
                radiusMeters = 0,
                hasNameError = false,
                isLoading = false
            )
        }
    }

}

sealed class CreatePlaceScreenEvent() {
    data class Error(val error: Throwable): CreatePlaceScreenEvent()
    object PlaceUpdated: CreatePlaceScreenEvent()
}