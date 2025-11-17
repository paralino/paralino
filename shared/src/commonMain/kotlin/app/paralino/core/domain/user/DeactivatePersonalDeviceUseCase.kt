package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.PersonalLocationDeletePayload
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository

class DeactivatePersonalDeviceUseCase(
    private val functionsApi: IFunctionsApi,
    private val personalSpaceRefresh: PersonalSpaceRefreshUseCase,
    private val locationSharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs
): BaseRepository() {
    suspend fun deactivateLocationInPersonalSpace(deviceId: String) {
        triggerRemoteOnly {
            val payload = PersonalLocationDeletePayload(
                deviceId = deviceId
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.LOCATION_DELETE_PERSONAL,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            } else {
                if (deviceId == appPrefs.deviceId) {
                    locationSharingManager.stopSharing(
                        groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                        deleteContract = false
                    )
                }
                personalSpaceRefresh.refreshPersonalSpace(cacheTimeMillis = 0)
            }
        }
    }
}