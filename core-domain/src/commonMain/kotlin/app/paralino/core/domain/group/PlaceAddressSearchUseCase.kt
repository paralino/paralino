package app.paralino.core.domain.group

import androidx.compose.ui.text.intl.Locale
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.raw.PlaceAddressSearchPayload
import app.paralino.core.models.raw.PlaceAddressSearchResponse
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.roundToPrecision

class PlaceAddressSearchUseCase(
    private val functionApi: IFunctionsApi,
    private val appPrefs: AppPrefs
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
        val location = if (appPrefs.sendLocationWithPlaceSearch) {
            latLng
        } else {
            null
        }

        return triggerRemoteOnly {
            val response = functionApi.runAndWait(
                function = ServerFunction.PLACE_ADDRESS_SEARCH,
                data = PlaceAddressSearchPayload(
                    address = address,
                    language = language,
                    lat = location?.lat?.toFloat()?.roundToPrecision(decimalPlaces = 2),
                    lng = location?.lng?.toFloat()?.roundToPrecision(decimalPlaces = 2)
                )
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            response.response<PlaceAddressSearchResponse>().data
        }
    }

    fun getDefaultRegionLocation(): LatLng? {
        val region = try {
            Locale.current.region
        } catch (e: Exception) {
            null
        }

        return countries.firstOrNull { it.code == region }?.let {
            LatLng(
                lat = it.lat,
                lng = it.lng
            )
        }
    }

    data class Country(
        val code: String,
        val lat: Double,
        val lng: Double
    )

    val countries = listOf(
        Country("AD", 42.500, 1.500),
        Country("AE", 24.000, 54.000),
        Country("AF", 33.000, 65.000),
        Country("AG", 17.050, -61.800),
        Country("AI", 18.250, -63.167),
        Country("AL", 41.000, 20.000),
        Country("AM", 40.000, 45.000),
        Country("AO", -12.500, 18.500),
        Country("AQ", -90.000, 0.000),
        Country("AR", -34.000, -64.000),
        Country("AS", -14.333, -170.000),
        Country("AT", 47.333, 13.333),
        Country("AU", -27.000, 133.000),
        Country("AW", 12.500, -69.967),
        Country("AX", 60.117, 19.900),
        Country("AZ", 40.500, 47.500),
        Country("BA", 44.000, 18.000),
        Country("BB", 13.167, -59.533),
        Country("BD", 24.000, 90.000),
        Country("BE", 50.833, 4.000),
        Country("BF", 13.000, -2.000),
        Country("BG", 43.000, 25.000),
        Country("BH", 26.000, 50.550),
        Country("BI", -3.500, 30.000),
        Country("BJ", 9.500, 2.250),
        Country("BL", 18.500, -63.417),
        Country("BM", 32.333, -64.750),
        Country("BN", 4.500, 114.667),
        Country("BO", -17.000, -65.000),
        Country("BQ", 12.180, -68.250),
        Country("BR", -10.000, -55.000),
        Country("BS", 24.250, -76.000),
        Country("BT", 27.500, 90.500),
        Country("BV", -54.433, 3.400),
        Country("BW", -22.000, 24.000),
        Country("BY", 53.000, 28.000),
        Country("BZ", 17.250, -88.750),
        Country("CA", 60.000, -95.000),
        Country("CC", -12.500, 96.833),
        Country("CD", 0.000, 25.000),
        Country("CF", 7.000, 21.000),
        Country("CG", -1.000, 15.000),
        Country("CH", 47.000, 8.000),
        Country("CI", 8.000, -5.000),
        Country("CK", -21.233, -159.767),
        Country("CL", -30.000, -71.000),
        Country("CM", 6.000, 12.000),
        Country("CN", 35.000, 105.000),
        Country("CO", 4.000, -72.000),
        Country("CR", 10.000, -84.000),
        Country("CU", 21.500, -80.000),
        Country("CV", 16.000, -24.000),
        Country("CW", 12.117, -68.933),
        Country("CX", -10.500, 105.667),
        Country("CY", 35.000, 33.000),
        Country("CZ", 49.750, 15.500),
        Country("DE", 51.000, 9.000),
        Country("DJ", 11.500, 43.000),
        Country("DK", 56.000, 10.000),
        Country("DM", 15.417, -61.333),
        Country("DO", 19.000, -70.667),
        Country("DZ", 28.000, 3.000),
        Country("EC", -2.000, -77.500),
        Country("EE", 59.000, 26.000),
        Country("EG", 27.000, 30.000),
        Country("EH", 24.500, -13.000),
        Country("ER", 15.000, 39.000),
        Country("ES", 40.000, -4.000),
        Country("ET", 8.000, 38.000),
        Country("FI", 64.000, 26.000),
        Country("FJ", -18.000, 175.000),
        Country("FK", -51.750, -59.000),
        Country("FM", 6.917, 158.250),
        Country("FO", 62.000, -7.000),
        Country("FR", 46.000, 2.000),
        Country("GA", -1.000, 11.750),
        Country("GB", 54.000, -2.000),
        Country("GD", 12.117, -61.667),
        Country("GE", 42.000, 43.500),
        Country("GF", 4.000, -53.000),
        Country("GG", 49.467, -2.583),
        Country("GH", 8.000, -2.000),
        Country("GI", 36.133, -5.350),
        Country("GL", 72.000, -40.000),
        Country("GM", 13.467, -16.567),
        Country("GN", 11.000, -10.000),
        Country("GP", 16.250, -61.583),
        Country("GQ", 2.000, 10.000),
        Country("GR", 39.000, 22.000),
        Country("GS", -54.500, -37.000),
        Country("GT", 15.500, -90.250),
        Country("GU", 13.467, 144.783),
        Country("GW", 12.000, -15.000),
        Country("GY", 5.000, -59.000),
        Country("HK", 22.267, 114.188),
        Country("HM", -53.100, 72.517),
        Country("HN", 15.000, -86.500),
        Country("HR", 45.167, 15.500),
        Country("HT", 19.000, -72.417),
        Country("HU", 47.000, 20.000),
        Country("ID", -5.000, 120.000),
        Country("IE", 53.000, -8.000),
        Country("IL", 31.470, 35.130),
        Country("IM", 54.250, -4.500),
        Country("IN", 20.000, 77.000),
        Country("IO", -6.000, 71.500),
        Country("IQ", 33.000, 44.000),
        Country("IR", 32.000, 53.000),
        Country("IS", 65.000, -18.000),
        Country("IT", 42.833, 12.833),
        Country("JE", 49.250, -2.167),
        Country("JM", 18.250, -77.500),
        Country("JO", 31.000, 36.000),
        Country("JP", 36.000, 138.000),
        Country("KE", 1.000, 38.000),
        Country("KG", 41.000, 75.000),
        Country("KH", 13.000, 105.000),
        Country("KI", 1.417, 173.000),
        Country("KM", -12.167, 44.250),
        Country("KN", 17.333, -62.750),
        Country("KP", 40.000, 127.000),
        Country("KR", 37.000, 127.500),
        Country("KW", 29.500, 45.750),
        Country("KY", 19.500, -80.500),
        Country("KZ", 48.000, 68.000),
        Country("LA", 18.000, 105.000),
        Country("LB", 33.833, 35.833),
        Country("LC", 13.883, -60.967),
        Country("LI", 47.267, 9.533),
        Country("LK", 7.000, 81.000),
        Country("LR", 6.500, -9.500),
        Country("LS", -29.500, 28.500),
        Country("LT", 56.000, 24.000),
        Country("LU", 49.750, 6.167),
        Country("LV", 57.000, 25.000),
        Country("LY", 25.000, 17.000),
        Country("MA", 32.000, -5.000),
        Country("MC", 43.733, 7.400),
        Country("MD", 47.000, 29.000),
        Country("ME", 42.500, 19.300),
        Country("MF", 18.083, -63.950),
        Country("MG", -20.000, 47.000),
        Country("MH", 9.000, 168.000),
        Country("MK", 41.833, 22.000),
        Country("ML", 17.000, -4.000),
        Country("MM", 22.000, 98.000),
        Country("MN", 46.000, 105.000),
        Country("MO", 22.167, 113.550),
        Country("MP", 15.200, 145.750),
        Country("MQ", 14.667, -61.000),
        Country("MR", 20.000, -12.000),
        Country("MS", 16.750, -62.200),
        Country("MT", 35.833, 14.583),
        Country("MU", -20.283, 57.550),
        Country("MV", 3.250, 73.000),
        Country("MW", -13.500, 34.000),
        Country("MX", 23.000, -102.000),
        Country("MY", 2.500, 112.500),
        Country("MZ", -18.250, 35.000),
        Country("NA", -22.000, 17.000),
        Country("NC", -21.500, 165.500),
        Country("NE", 16.000, 8.000),
        Country("NF", -29.033, 167.950),
        Country("NG", 10.000, 8.000),
        Country("NI", 13.000, -85.000),
        Country("NL", 52.500, 5.750),
        Country("NO", 62.000, 10.000),
        Country("NP", 28.000, 84.000),
        Country("NR", -0.533, 166.917),
        Country("NU", -19.033, -169.867),
        Country("NZ", -41.000, 174.000),
        Country("OM", 21.000, 57.000),
        Country("PA", 9.000, -80.000),
        Country("PE", -10.000, -76.000),
        Country("PF", -15.000, -140.000),
        Country("PG", -6.000, 147.000),
        Country("PH", 13.000, 122.000),
        Country("PK", 30.000, 70.000),
        Country("PL", 52.000, 20.000),
        Country("PM", 46.833, -56.333),
        Country("PN", -25.067, -130.100),
        Country("PR", 18.250, -66.500),
        Country("PS", 31.900, 35.200),
        Country("PT", 39.500, -8.000),
        Country("PW", 7.500, 134.500),
        Country("PY", -23.000, -58.000),
        Country("QA", 25.500, 51.250),
        Country("RE", -21.150, 55.500),
        Country("RO", 46.000, 25.000),
        Country("RS", 44.000, 21.000),
        Country("RU", 60.000, 100.000),
        Country("RW", -2.000, 30.000),
        Country("SA", 25.000, 45.000),
        Country("SB", -8.000, 159.000),
        Country("SC", -4.583, 55.667),
        Country("SD", 15.000, 30.000),
        Country("SE", 62.000, 15.000),
        Country("SG", 1.367, 103.800),
        Country("SH", -15.950, -5.720),
        Country("SI", 46.117, 14.817),
        Country("SJ", 78.000, 20.000),
        Country("SK", 48.667, 19.500),
        Country("SL", 8.500, -11.500),
        Country("SM", 43.767, 12.417),
        Country("SN", 14.000, -14.000),
        Country("SO", 10.000, 49.000),
        Country("SR", 4.000, -56.000),
        Country("SS", 7.000, 30.000),
        Country("ST", 1.000, 7.000),
        Country("SV", 13.833, -88.917),
        Country("SX", 18.033, -63.050),
        Country("SY", 35.000, 38.000),
        Country("SZ", -26.500, 31.500),
        Country("TC", 21.750, -71.583),
        Country("TD", 15.000, 19.000),
        Country("TF", -49.250, 69.167),
        Country("TG", 8.000, 1.167),
        Country("TH", 15.000, 100.000),
        Country("TJ", 39.000, 71.000),
        Country("TK", -9.000, -172.000),
        Country("TL", -8.833, 125.917),
        Country("TM", 40.000, 60.000),
        Country("TN", 34.000, 9.000),
        Country("TO", -20.000, -175.000),
        Country("TR", 39.000, 35.000),
        Country("TT", 11.000, -61.000),
        Country("TV", -8.000, 178.000),
        Country("TW", 23.500, 121.000),
        Country("TZ", -6.000, 35.000),
        Country("UA", 49.000, 32.000),
        Country("UG", 1.000, 32.000),
        Country("UM", 19.300, 166.633),
        Country("US", 38.000, -97.000),
        Country("UY", -33.000, -56.000),
        Country("UZ", 41.000, 64.000),
        Country("VA", 41.900, 12.450),
        Country("VC", 13.250, -61.200),
        Country("VE", 8.000, -66.000),
        Country("VG", 18.431, -64.623),
        Country("VI", 18.350, -64.933),
        Country("VN", 16.167, 107.833),
        Country("VU", -16.000, 167.000),
        Country("WF", -13.300, -176.200),
        Country("WS", -13.583, -172.333),
        Country("XK", 42.667, 21.167),
        Country("YE", 15.000, 48.000),
        Country("YT", -12.833, 45.167),
        Country("ZA", -29.000, 24.000),
        Country("ZM", -15.000, 30.000),
        Country("ZW", -20.000, 30.000),
    )
}