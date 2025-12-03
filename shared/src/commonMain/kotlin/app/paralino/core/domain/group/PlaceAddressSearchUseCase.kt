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
            Locale.current.language
        } catch (e: Exception) {
            "en"
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
//            delay(300)
//            PlaceAddressSearchResponse(
//                results = listOf(
//                    PlaceAddressSearchResult(
//                        title = "Eiffel Tower",
//                        address = "Paris 7th arrondissement, France",
//                        lat = 48.8584,
//                        lng = 2.3584
//                    ),
//                    PlaceAddressSearchResult(
//                        title = "Louvre Museum",
//                        address = "Paris 1st arrondissement, France",
//                        lat = 48.8606,
//                        lng = 2.3522
//                    ),
//                    PlaceAddressSearchResult(
//                        title = "Notre-Dame Cathedral",
//                        address = "Paris 4th arrondissement, France",
//                        lat = 48.8534,
//                        lng = 2.3488
//                    ),
//                    PlaceAddressSearchResult(
//                        title = "Arc de Triomphe",
//                        address = "Paris 8th arrondissement, France",
//                        lat = 48.8738,
//                        lng = 2.3091
//                    ),
//                    PlaceAddressSearchResult(
//                        title = "Eiffel Tower",
//                        address = "Paris 7th arrondissement, France",
//                        lat = 48.8584,
//                        lng = 2.3584
//                    ),
//                    PlaceAddressSearchResult(
//                        title = "Louvre Museum",
//                        address = "Paris 1st arrondissement, France",
//                        lat = 48.8606,
//                        lng = 2.3522
//                    ),
//                    PlaceAddressSearchResult(
//                        title = "Notre-Dame Cathedral",
//                        address = "Paris 4th arrondissement, France",
//                        lat = 48.8534,
//                        lng = 2.3488
//                    ),
//                    PlaceAddressSearchResult(
//                        title = "Arc de Triomphe",
//                        address = "Paris 8th arrondissement, France",
//                        lat = 48.8738,
//                        lng = 2.3091
//                    )
//                )
//            )
        }
    }
}