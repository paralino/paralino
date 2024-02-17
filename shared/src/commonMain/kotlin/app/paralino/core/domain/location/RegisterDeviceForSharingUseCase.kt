package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.raw.LocationCreatePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString

class RegisterDeviceForSharingUseCase constructor(
    private val functionsApi: IFunctionsApi,
    private val userMemberId: UserMemberIdUseCase,
    private val groupLocal: UserGroupLocalDataSource,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val groupPreRefreshUseCase: GroupPreRefreshUseCase,
    private val cryptoCore: CryptoCore,
    private val deviceKeyRotation: MemberDeviceKeyRotation,
    private val deviceStatusProvider: MemberDeviceStatusProvider
) : LimitedExecutionRepository() {

    suspend fun isDeviceAlreadyRegistered(groupId: String): Boolean {
        return userMemberId.hasCurrentLocationIdForGroup(groupId)
    }

    suspend fun registerDevice(
        groupId: String,
        deviceName: String?,
        contract: LocationSharingContract?
    ) {
        startProcessing(groupId)

        try {
            triggerRemoteOnly {
                // refresh all group data before sharing starts
                logg.tag("sharing_start").d("Start location register, refresh group data")
                val result1 = groupPreRefreshUseCase.refreshGroupWithMembers(groupId)
                logg.tag("sharing_start").d("Group refreshed ($result1), fetch group")
                val group = groupLocal.getGroupWithMembers(groupId).first()

                val member = userMemberId.getCurrentUserMemberForGroup(groupId)
                    ?: throw IllegalStateException("Missing member for a group $groupId")

                val groupKey = cryptoKEKRepo.getGroupKey(groupId, member.groupKeyVersion)

                if (groupKey !is GroupMasterKey.Valid)
                    throw MissingGroupKeyException(member.groupId, member.groupKeyVersion)

                // always rotate keys for every sharing session
                val keyData = deviceKeyRotation.getNewSenderKeys(group = group)

                val deviceData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        MemberDeviceData(
                            deviceName = deviceName ?: userMemberId.getDefaultDeviceName(groupId),
                            deviceType = userMemberId.getCurrentDeviceType().key
                        )
                    ),
                    key = groupKey.key
                )

                val status = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        deviceStatusProvider.getStatus(
                            groupId = groupId,
                            contract = contract
                        )
                    ),
                    key = keyData.first
                )

                val payload = LocationCreatePayload(
                    groupId = groupId,
                    deviceId = config.deviceId,
                    deviceData = deviceData,
                    status = status,
                    groupKeyVersion = groupKey.version,
                    keys = keyData.second
                )

                logg.tag("sharing_start").d("Send register device request")
                val response = functionsApi.runAndWait(
                    function = ServerFunction.LOCATION_CREATE,
                    data = payload
                )

                if (response.isSuccess) {
                    logg.tag("sharing_start").d("Device registered, save integrity")

                    val groupDeviceIntegrity = GroupIntegrity(
                        timeUtc = nowUTC().toString(),
                        signature = GroupIntegrity.createSignature(
                            hashProvider = cryptoCore.hashProvider,
                            serverIntegrity = group.group.serverIntegrity,
                            memberVerifications = group.members.map { it.member.verification },
                            encryptedMemberPublicKeys = cryptoKEKRepo.getAllEncryptedMembershipPublicKeys(
                                groupId = group.id
                            )
                        )
                    ).let {
                        cryptoCore.encryptDataWithKey(
                            data = BaseRemoteModule.formatter.encodeToString(it),
                            key = userKeyRepo.personalKey()
                        )
                    }
                    cryptoKEKRepo.updateIntegrity(
                        groupId = group.id,
                        integrity = groupDeviceIntegrity
                    )

                    logg.tag("sharing_start").d("Integrity updated, refresh group")
                    val result2 = groupPreRefreshUseCase.refreshGroupWithMembers(groupId, cacheTimeMillis = 0)
                    logg.tag("sharing_start").d("Group refreshed ($result2)")
                } else {
                    logg.tag("sharing_start").w("Device not registered")
                    // TODO throw better exception
                    throw BaseRemoteException.fromCode(
                        code = response.statusCode,
                        message = "Unable to register device to group $groupId",
                        cause = Exception(response.stderr)
                    )
                }
            }
        } finally {
            stopProcessing(groupId)
        }
    }
}