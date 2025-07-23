package app.paralino.core.repo.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.raw.UpdateUserDeviceFunctionData
import app.paralino.core.models.raw.UserDeviceLocationRaw
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.models.raw.UserDeviceWithLocationRaw
import app.paralino.core.models.raw.UserDevicesWithLocationsAndPlacesRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.places.PlacesRemoteSource
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.async

class UserDevicesRemoteSource constructor(
    private val documentApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val appBuildConfig: AppBuildConfig,
    private val userMemberIdUseCase: UserMemberIdUseCase,
    private val placesRemote: PlacesRemoteSource,
    private val appPrefs: AppPrefs
) {

    suspend fun getAllPersonalSpaceData(): UserDevicesWithLocationsAndPlacesRaw {
        return blocking {

            val placesAsync = async { placesRemote.getAllPersonalPlaces() }

            val personalDevicesAsync = async {
                documentApi.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionUserDevicesID,
                ).map { it.parsePersonalDevice() }
            }

            val personalLocationsAsync = async {
                documentApi.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionPersonalLocationsID,
                ).map { it.parsePersonalDeviceLocation() }
            }

            val personalDevices = personalDevicesAsync.await()
            val personalLocations = personalLocationsAsync.await()
            val places = placesAsync.await()

            UserDevicesWithLocationsAndPlacesRaw(
                devices = personalDevices.map { device ->
                    UserDeviceWithLocationRaw(
                        device = device,
                        location = personalLocations.firstOrNull { it.id == device.id }
                    )
                },
                places = places.filter {
                    it.place.isInPersonalSpace == true
                }
            )
        }
    }

    suspend fun updateDeviceData(pushToken: String, locationPushToken: String?) {
        val payload = UpdateUserDeviceFunctionData(
            sessionId = appPrefs.sessionId,
            deviceId = appPrefs.deviceId,
            pushToken = pushToken,
            deviceType = userMemberIdUseCase.getCurrentDeviceType().key,
            locationPushToken = locationPushToken
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.DEVICE_UPDATE,
            data = payload
        )

        if (!response.isSuccess) {
            throw response.remoteException
        }
    }

    private fun Map<String, Any>.parsePersonalDeviceLocation(): UserDeviceLocationRaw {
        return UserDeviceLocationRaw.from(this)
    }

    private fun Map<String, Any>.parsePersonalDevice(): UserDeviceRaw {
        return UserDeviceRaw.from(this)
    }
}