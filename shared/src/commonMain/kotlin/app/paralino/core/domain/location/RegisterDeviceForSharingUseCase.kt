package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.raw.LocationCreatePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import kotlinx.serialization.encodeToString

class RegisterDeviceForSharingUseCase constructor(
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val memberRemote: LocationRemoteSource,
    private val userMemberId: UserMemberIdUseCase,
    private val groupLocal: UserGroupLocalDataSource,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore
) : BaseRepository() {

    suspend fun canRegister(groupId: String): Boolean {
        return userMemberId.hasCurrentLocationIdForGroup(groupId)
    }

    suspend fun registerDevice(
        groupId: String
    ) {
        triggerRemoteOnly {

            if (userMemberId.hasCurrentLocationIdForGroup(groupId)) {
                // current device is already registered
                return@triggerRemoteOnly
            }

            val member = userMemberId.getCurrentUserMemberForGroup(groupId)
                ?: throw IllegalStateException("Missing member for a group $groupId")

            val groupKey = cryptoKEKRepo.getGroupKey(groupId, member.groupKeyVersion)

            if(groupKey !is GroupMasterKey.Valid)
                throw MissingGroupKeyException(member.groupId, member.groupKeyVersion)

            val deviceData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberDeviceData(
                        deviceName = config.deviceName,
                        deviceType = userMemberId.getCurrentDeviceType().key
                    )
                ),
                key = groupKey.key
            )

            val payload = LocationCreatePayload(
                groupId = groupId,
                deviceId = userMemberId.getCurrentDeviceId(),
                deviceData = deviceData,
            )

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionLocationCreate,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (data.isSuccess) {
                memberRemote.getMember(userMemberId.getCurrentUserMemberIdForGroup(groupId)!!)
                    .also { groupLocal.updateOrCreateMember(it) }
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