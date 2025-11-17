package app.paralino.core.presentation.places.create

import androidx.compose.ui.text.AnnotatedString
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis

data class CreatePlaceScreenState constructor(
    val groupId: GroupOrSpaceId,
    val isNewPlace: Boolean,
    val name: String,
    val image: ImageModel,
    val private: Boolean,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
    val radiusText: String,
    val hintText: AnnotatedString,
    val hasNameError: Boolean,
    val actionLoading: Boolean,
    val fullLoading: Boolean,
    val alertDialog: MainAlertDialog?
) {

    val currentStep: Int = radiusSteps.indexOfFirst { radiusMeters <= it }.takeIf { it >= 0 } ?: 0
    companion object {

        const val defaultRadiusMeters = 200
        val radiusSteps = arrayOf(
            50, 80, 100, 150, 200, 300, 400, 500, 600, 700, 800, 900, 1000,
            1200, 1500, 1800, 2000, 2200, 2500, 2800, 3000, 3500, 4000, 4500,
            5000, 6000, 7000, 8000, 9000, 10000, 12500, 15000, 17500, 20000
        )

        fun radiusForIndex(index: Int): Int {
            return radiusSteps.getOrNull(index) ?: defaultRadiusMeters
        }

        fun empty(
            groupId: GroupOrSpaceId,
            hintText: AnnotatedString,
            lat: Double? = null,
            lng: Double? = null,
            radiusMeters: Int,
            radiusText: String
        ): CreatePlaceScreenState {
            return CreatePlaceScreenState(
                groupId = groupId,
                isNewPlace = true,
                name = "",
                image = ImageModel.emoji(Emojis.circles.random()),
                private = false,
                lat = lat,
                lng = lng,
                radiusMeters = radiusMeters,
                radiusText = radiusText,
                hintText = hintText,
                hasNameError = false,
                actionLoading = false,
                fullLoading = false,
                alertDialog = null
            )
        }
    }

}

sealed class CreatePlaceScreenEvent() {
    object MissingInfo: CreatePlaceScreenEvent()
    object MissingLocation: CreatePlaceScreenEvent()
    object PlaceUpdated: CreatePlaceScreenEvent()
}

sealed interface CreatePlaceScreenUiEvent {
    data object CreatePlace : CreatePlaceScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : CreatePlaceScreenUiEvent
}