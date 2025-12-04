package app.paralino.core.domain.group

import androidx.compose.ui.text.intl.Locale
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.raw.PlaceAddressSearchPayload
import app.paralino.core.models.raw.PlaceAddressSearchResponse
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.roundToPrecision

class PlaceAddressSearchUseCase(
    private val functionApi: IFunctionsApi
) : BaseRepository() {
    suspend fun searchAddress(
        address: String,
        latLng: LatLng?
    ): PlaceAddressSearchResponse {
        val language = try {
            Locale.current.toLanguageTag()
        } catch (e: Exception) {
            "en-US"
        }
        return triggerRemoteOnly {
            val response = functionApi.runAndWait(
                function = ServerFunction.PLACE_ADDRESS_SEARCH,
                data = PlaceAddressSearchPayload(
                    address = address,
                    language = language,
                    lat = latLng?.lat?.toFloat()?.roundToPrecision(decimalPlaces = 2),
                    lng = latLng?.lng?.toFloat()?.roundToPrecision(decimalPlaces = 2)
                )
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            response.response<PlaceAddressSearchResponse>().data
        }
    }
}