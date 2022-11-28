package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.raw.LocationCreatePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.location.ILocationLocalDataSource
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
    private val memberLocal: ILocationLocalDataSource,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore
) : BaseRepository() {

    suspend fun registerDevice(
        groupId: String
    ) {
        triggerRemoteOnly {

            if (!userMemberId.getCurrentLocationIdForGroup(groupId).isNullOrBlank()) {
                // current device is already registered
                return@triggerRemoteOnly
            }

            val member = userMemberId.getCurrentUserMemberForGroup(groupId)
                ?: throw IllegalStateException("Missing member for a group $groupId")

            val groupKey = cryptoKEKRepo.getGroupKey(groupId, member.member.groupKeyVersion)
                ?: throw MissingGroupKeyException(
                    member.member.groupId,
                    member.member.groupKeyVersion
                )

            val deviceData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberDeviceData(
                        deviceName = config.deviceName,
                        deviceType = "phone"
                    )
                ),
                key = groupKey.data.content
            )

            val payload = LocationCreatePayload(
                groupId = groupId,
                deviceData = deviceData,
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