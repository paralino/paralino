package app.paralino.core.domain.location

import app.paralino.core.models.BackendConfig
import app.paralino.core.models.raw.LocationCreatePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import kotlinx.serialization.encodeToString

class RegisterDeviceForSharingUseCase constructor(
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val memberRemote: LocationRemoteSource,
    private val userMemberId: UserMemberIdUseCase,
    private val memberLocal: ILocationLocalDataSource
): BaseRepository() {

    suspend fun registerDevice(
        groupId: String,
    ) {
        triggerRemoteOnly {

            if(!userMemberId.getCurrentLocationIdForGroup(groupId).isNullOrBlank()) {
                // current device is already registered
                return@triggerRemoteOnly
            }

            val payload = LocationCreatePayload(
                groupId = groupId,
                // TODO add device data
                deviceData = "",
                deviceId = userMemberId.getCurrentDeviceId(),
            )

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionLocationCreate,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (data.isSuccess) {
                memberRemote.getMember(userMemberId.getCurrentUserMemberIdForGroup(groupId)!!)
                    .also { memberLocal.recreateMembers(it) }
            } else {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = null,
                    message = "Unable to register device to group $groupId",
                    cause = Exception(data.stderr)
                )
            }
        }
    }

}