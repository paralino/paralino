package app.paralino.core.domain.location

import app.paralino.core.domain.location.history.SavePendingLocationUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.places.PlaceActivityUploadEngine

class MemberDeviceStatusProcessor(
    private val saveLocationUseCase: SaveLocationUseCase,
    private val savePendingLocationUseCase: SavePendingLocationUseCase,
    private val payloadProvider: LocationPayloadProvider,
    private val sharingManager: LocationSharingManager,
    private val oneTimeLocationProvider: IOneTimeLocationProvider,
    private val batteryAlertUploadEngine: BatteryAlertUploadEngine,
    private val placeActivityUploadEngine: PlaceActivityUploadEngine,
) : BaseRepository() {

    suspend fun pushDeviceStatus(groupId: GroupOrSpaceId) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "device_status",
                timeMillis = 1_000
            )
        ) {
            val payload = payloadProvider.getLocationPayload(
                groupId = groupId,
                data = oneTimeLocationProvider.getLastLocation(IOneTimeLocationProvider.RequestType.CacheOnly),
                contract = sharingManager.currentSharingContractForGroup(groupId)
            ).payload

            if (payload != null) {
                logg.v("device_status","Update device status in group $groupId with location (${payload.locationData != null})")
                try {
                    saveLocationUseCase.uploadAndSaveCurrentLocation(MemberGroupIdForUpload(payload))
                } finally {
                    savePendingLocationUseCase.saveNewLocations(listOf(payload))

                    // trigger sync for pending data regardless of error
                    batteryAlertUploadEngine.sendBatteryAlert(payloads = listOf(payload))
                    placeActivityUploadEngine.syncPendingEventsInBackground()
                }
            }
        }
    }
}