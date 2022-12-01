package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.Payload
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.UpdateMemberData
import app.paralino.core.models.raw.UpdateMemberFunctionPayload
import app.paralino.core.models.user.UserMeData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRemoteSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class UpdateUserDataUseCase constructor(
    private val userRepo: UserRepository,
    private val userRemoteSource: UserRemoteSource,
    private val locationRepo: LocationRepository,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val personalCodes: PersonalCodesRepository,
    private val userMemberIdUseCase: UserMemberIdUseCase
) : BaseRepository() {

    fun updateUserData(data: UserMeData, deviceName: String): Flow<Boolean> {
        return getDataOnce(
            remote = { userRemoteSource.updateUserData(data) },
            localUpdate = { userRepo.updateLocalUser(it) }
        )
            .map {
                updateMemberDataInGroups(data, deviceName)
            }
            .flatMapLatest {
                config.deviceName = deviceName
                personalCodes.revokeAllCodes()
            }
            .map { it is Payload.Success }
    }

    private suspend fun updateMemberDataInGroups(data: UserMeData, deviceName: String) {
        val userMembers = locationRepo.getMembersByUserId(userRepo.cachedUserId).first()

        val nodes = userMembers.map { member ->

            val groupKey = cryptoKEKRepo.getGroupKey(member.member.groupId, member.member.groupKeyVersion)
                ?: throw MissingGroupKeyException(member.member.groupId, member.member.groupKeyVersion)

            val memberData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelData(data.userName, data.userImage)
                ),
                key = groupKey.data.content
            )

            val deviceData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberDeviceData(
                        deviceName = deviceName,
                        deviceType = "phone"
                    )
                ),
                key = groupKey.data.content
            )

            UpdateMemberData(
                memberId = member.member.id,
                locationId = userMemberIdUseCase.getCurrentLocationIdForGroup(member.member.groupId) ?: "",
                groupKeyVersion = member.member.groupKeyVersion,
                memberData = memberData,
                deviceData = deviceData
            )
        }

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

}