package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
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
) : BaseRepository() {

    fun updateUserData(data: UserMeData): Flow<Unit> {
        return getDataOnce(
            remote = {
                val newData = userRemoteSource.updateUserData(data)
                updateMemberDataInGroups(data)
                personalCodes.updateUserCode(data)
                newData
            },
            localUpdate = {
                userRepo.updateLocalUser(it)
            }
        )
            .map { Unit }
    }

    private suspend fun updateMemberDataInGroups(data: UserMeData) {
        val userMembers = locationRepo.getMembersByUserId(userRepo.cachedUserId)

        val nodes = userMembers.map { member ->

            val groupKey = cryptoKEKRepo.getGroupKey(
                groupId = member.groupId,
                keyVersion = member.groupKeyVersion
            )

            if(groupKey !is GroupMasterKey.Valid)
                throw MissingGroupKeyException(member.groupId, member.groupKeyVersion)

            val memberData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelData(data.userName, data.userImage)
                ),
                key = groupKey.key
            )

            UpdateMemberData(
                memberId = member.id,
                groupKeyVersion = member.groupKeyVersion,
                memberData = memberData,
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

        val newPushToken = config.pushTokenUnsynced
            .takeIf { it.isNotBlank() && it != config.pushTokenSynced }
            ?: let {
                config.pushTokenUnsynced = ""
                logg.tag("push_notifications").v("No new push token to register")
                return
            }

        logg.tag("push_notifications").v("Found unsynced push token, syncing...")

        userDevicesRemoteSource.updateDeviceData(newPushToken)
        config.pushTokenUnsynced = ""
        config.pushTokenSynced = newPushToken

        logg.tag("push_notifications").v("Push token registered")
    }

}