package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.UpdateMemberData
import app.paralino.core.models.raw.UpdateMemberFunctionPayload
import app.paralino.core.models.user.UserMeData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserDevicesRemoteSource
import app.paralino.core.repo.user.UserRemoteSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class UpdateUserDataUseCase constructor(
    private val userRepo: UserRepository,
    private val userRemoteSource: UserRemoteSource,
    private val userDevicesRemoteSource: UserDevicesRemoteSource,
    private val locationRepo: LocationRepository,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val personalCodes: PersonalCodesRepository,
    private val userMemberIdUseCase: UserMemberIdUseCase
) : BaseRepository() {

    fun updateUserData(data: UserMeData, deviceName: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                val newData = userRemoteSource.updateUserData(data)
                updateMemberDataInGroups(data, deviceName)
                personalCodes.revokeAllCodes(createNewCode = true, userMeData = data)
                newData
            },
            localUpdate = {
                config.deviceName = deviceName
                userRepo.updateLocalUser(it)
            }
        )
            .map { Unit }
    }

    private suspend fun updateMemberDataInGroups(data: UserMeData, deviceName: String) {
        val userMembers = locationRepo.getMembersByUserId(userRepo.cachedUserId)

        val nodes = userMembers.map { member ->

            val groupKey = cryptoKEKRepo.getGroupKey(
                groupId = member.groupId,
                keyVersion = member.groupKeyVersion
            ) ?: throw MissingGroupKeyException(
                groupId = member.groupId,
                keyVersion = member.groupKeyVersion
            )

            val memberData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelData(data.userName, data.userImage)
                ),
                key = groupKey.data
            )

            val deviceData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberDeviceData(
                        deviceName = deviceName,
                        deviceType = userMemberIdUseCase.getCurrentDeviceType().key
                    )
                ),
                key = groupKey.data
            )

            UpdateMemberData(
                memberId = member.id,
                locationId = userMemberIdUseCase.getCurrentLocationIdForGroup(member.groupId) ?: "",
                groupKeyVersion = member.groupKeyVersion,
                memberData = memberData,
                deviceData = deviceData
            )
        }

        if (nodes.isEmpty())
            return

        val payload = UpdateMemberFunctionPayload(
            data = nodes
        )

        val response = functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionGroupMemberUpdateID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )

        if (!response.isSuccess) {
            // TODO throw better exception
            throw BaseRemoteException.fromCode(
                code = null,
                message = "Unable to update location",
                cause = Exception(response.stderr)
            )
        }
    }

    suspend fun registerPushTokenIfNeeded() {
        userRepo.cachedUser?.data ?: let {
            logg.tag("push_notifications").v("User not present, ignoring...")
            return
        }

        val unsyncedToken = config.pushTokenUnsynced
            .takeIf { it.isNotBlank() }
            ?: let {
                logg.tag("push_notifications").v("No new push token to register")
                return
            }

        logg.tag("push_notifications").v("Found unsynced push token, syncing...")

        config.pushTokenUnsynced = unsyncedToken
        userDevicesRemoteSource.updateDeviceData(unsyncedToken)
        config.pushTokenUnsynced = ""

        logg.tag("push_notifications").v("Push token registered")
    }

}