package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

class RevokeDeviceActiveStatusUseCase(
    private val local: GetMemberWithLocationsUseCase,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val sharingManager: LocationSharingManager,
    private val memberDeviceStatusProcessor: MemberDeviceStatusProcessor,
    private val appPrefs: AppPrefs
) : BaseRepository() {
    suspend fun revokeActiveStatus() {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "device_status_reset",
                timeMillis = 10.minutes.inWholeMilliseconds
            )
        ) {
            val memberModels = local.getMemberModelsWithLocationForUser(appPrefs.userId)
                .mapNotNull { it.currentDeviceLocation }
                .let {
                    val personalSpaceLocation = personalSpaceRepo.getUserDeviceByDeviceIdOnce(
                        deviceId = appPrefs.deviceId
                    )?.location
                    if (personalSpaceLocation != null) {
                        it.plus(personalSpaceLocation)
                    } else {
                        it
                    }
                }
                .filter { location ->
                    val sharingContract = sharingManager.unfilteredContractForGroup(id = location.groupOrSpaceId)
                    val sharingActive = sharingContract?.asValidContract() != null

                    val statusActive = location.locationAwareStatusType?.let {
                        if (sharingContract?.shareDeviceStatus == true) {
                            it != MemberDeviceStatusType.NOT_SHARING
                        } else if (sharingContract?.shareDeviceStatus == false){
                            it != MemberDeviceStatusType.INACTIVE
                        } else {
                            // in case sharing contract is not present (regardless if valid or not)
                            // we continue with false which will not revoke the status
                            // this way it stays as it is since we don't know what can be set safely
                            false
                        }
                    } ?: false
                    !sharingActive && statusActive
                }

            logg.d("Revoking device status for ${memberModels.size} groups")
            memberModels.forEach {
                try {
                    memberDeviceStatusProcessor.pushDeviceStatus(groupId = it.groupOrSpaceId)
                } catch (e: Exception) {
                    logg.w("Error while resetting device status", e)
                }
                delay(300)
            }
        }
    }
}