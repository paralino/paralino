package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.Payload
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.LocationNodePayload
import app.paralino.core.models.raw.UpdateLocationFunctionData
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
    private val personalCodes: PersonalCodesRepository
) : BaseRepository() {

    fun updateUserData(data: UserMeData): Flow<Boolean> {
        return getDataOnce(
            remote = {
                val pushToken = config.pushTokenUnsynced
                    .takeIf { it.isNotBlank() }
                    ?: data.pushToken.takeIf { it.isNotBlank() }
                    ?: userRepo.cachedUser?.data?.pushToken
                userRemoteSource.updateUserData(data.copy(pushToken = pushToken ?: ""))
            },
            localUpdate = { userRepo.updateLocalUser(it) }
        )
            .map {
                updateMemberDataInGroups(data)
            }
            .flatMapLatest {
                personalCodes.revokeAllCodes()
            }
            .map { it is Payload.Success }
    }

    private suspend fun updateMemberDataInGroups(data: UserMeData) {
        val userMembers = locationRepo.getMembersByUserId(userRepo.cachedUserId).first()

        val nodes = userMembers.map { member ->

            val groupKey = cryptoKEKRepo.getGroupKey(member.member.groupId, member.member.groupKeyVersion)
                ?: throw MissingGroupKeyException(member.member.groupId, member.member.groupKeyVersion)

            val newData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelData(data.userName, data.userImage)
                ),
                key = groupKey.data.content
            )

            LocationNodePayload(
                groupId = member.member.groupId,
                groupKeyVersion = member.member.groupKeyVersion,
                memberKeyVersion = member.member.memberKeyVersion,
                memberData = newData,
                locations = emptyList()
            )
        }

        val payload = UpdateLocationFunctionData(
            data = nodes
        )

        val response = functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionLocationUpdateID,
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