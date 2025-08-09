package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.GroupValidityVerification
import app.paralino.core.domain.crypto.IMemberDeviceKeyRotation
import app.paralino.core.domain.crypto.IntegrityResult
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.raw.LocationCreateGroupPayload
import app.paralino.core.models.raw.LocationCreatePersonalPayload
import app.paralino.core.provider.current
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DeviceSafetySignatureCorrupted
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.seconds

class RegisterDeviceForSharingUseCase constructor(
    private val functionsApi: IFunctionsApi,
    private val userMemberId: UserMemberIdUseCase,
    private val groupLocal: UserGroupLocalDataSource,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val groupPreRefreshUseCase: GroupPreRefreshUseCase,
    private val personalSpaceRefreshUseCase: PersonalSpaceRefreshUseCase,
    private val cryptoCore: CryptoCore,
    private val deviceKeyRotation: IMemberDeviceKeyRotation,
    private val deviceStatusProvider: MemberDeviceStatusProvider,
    private val validityVerification: GroupValidityVerification,
    private val appPrefs: AppPrefs
) : LimitedExecutionRepository() {

    suspend fun isDeviceAlreadyRegistered(groupId: GroupOrSpaceId): Boolean {
        if (groupId.isPersonalSpace) {
            return personalSpaceRepo.personalSpaceState.value.data?.currentDevice?.location != null
        } else {
            return userMemberId.hasCurrentLocationIdForGroup(groupId.id)
        }
    }

    suspend fun registerDevice(
        groupId: GroupOrSpaceId,
        deviceName: String?,
        contract: LocationSharingContract?,
        notifyOthers: Boolean,
        enforceSafetySignature: Boolean
    ) {
        if (groupId.isPersonalSpace) {
            registerDeviceForPersonalSpace(
                deviceName = deviceName,
                contract = contract,
                notifyOthers = notifyOthers,
            )
        } else {
            registerDeviceForGroupSharing(
                groupId = groupId.id,
                deviceName = deviceName,
                contract = contract,
                notifyOthers = notifyOthers,
                enforceSafetySignature = enforceSafetySignature
            )
        }
    }

    private suspend fun registerDeviceForPersonalSpace(
        deviceName: String?,
        contract: LocationSharingContract?,
        notifyOthers: Boolean
    ) {
        startProcessing(UserGroup.groupPersonalDevicesId)

        try {
            triggerRemoteOnly {
                // refresh all group data before sharing starts
                logg.tag("sharing_start").d("Start location register, refresh group data")
                val result1 = personalSpaceRefreshUseCase.refreshPersonalSpace(cacheTimeMillis = 10.seconds.inWholeMilliseconds)
                logg.tag("sharing_start").d("Group refreshed ($result1), fetch group")
                val space = personalSpaceRepo.getPersonalSpaceWithDevices().firstOrNull()
                    ?: throw IllegalStateException("Personal space not valid")

                val userKey = userKeyRepo.activePersonalKeyWithVersion()

                // always rotate keys for every sharing session
                val newKey = cryptoCore.getNewSymmetricKeyForLocationData()

                val deviceData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        MemberDeviceData(
                            deviceName = deviceName ?: userMemberId.getDefaultDeviceName(null),
                            deviceType = userMemberId.getCurrentDeviceType().key
                        )
                    ),
                    key = userKey.key
                )

                val status = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        deviceStatusProvider.getStatus(
                            groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                            contract = contract
                        )
                    ),
                    key = newKey
                )

                val payload = LocationCreatePersonalPayload(
                    deviceId = appPrefs.deviceId,
                    deviceType = DeviceType.current.key,
                    sessionId = appPrefs.sessionId,
                    deviceData = deviceData,
                    // TODO pass location data already
                    locationData = null,
                    status = status,
                    notify = notifyOthers,
                    userKeyVersion = userKey.version,
                    encryptedSenderKey = cryptoCore.encryptDataWithKey(
                        data = newKey,
                        key = userKey.key
                    )
                )

                logg.tag("sharing_start").d("Send register device request")
                val response = functionsApi.runAndWait(
                    function = ServerFunction.LOCATION_CREATE_PERSONAL,
                    data = payload
                )

                if (response.isSuccess) {
                    logg.tag("sharing_start").d("Device registered, save integrity")

                    val groupDeviceIntegrity = GroupIntegrity(
                        timeUtc = nowUTC().toString(),
                        signature = GroupIntegrity.createSignature(
                            hashProvider = cryptoCore.hashProvider,
                            serverIntegrity = space.currentDevice?.sessionId ?: "",
                            memberVerifications = listOf(userKeyRepo.signatureKeyPair().publicKey),
                            encryptedMemberPublicKeys = listOf(userKeyRepo.signatureKeyPair().publicKey)
                        )
                    ).let {
                        cryptoCore.encryptDataWithKey(
                            data = BaseRemoteModule.formatter.encodeToString(it),
                            key = userKey.key
                        )
                    }

                    cryptoKEKRepo.updateIntegrity(
                        groupId = UserGroup.groupPersonalDevicesId,
                        integrity = groupDeviceIntegrity,
                        safetySignature = "",
                        userKeyVersion = userKey.version
                    )

                    logg.tag("sharing_start").d("Integrity updated, refresh space")
                    val result2 = personalSpaceRefreshUseCase.refreshPersonalSpace(
                        cacheTimeMillis = 0
                    )
                    logg.tag("sharing_start").d("Space refreshed ($result2)")
                } else {
                    logg.tag("sharing_start").w("Device not registered")
                    throw response.remoteException
                }
            }
        } finally {
            stopProcessing(UserGroup.groupPersonalDevicesId)
        }
    }

    private suspend fun registerDeviceForGroupSharing(
        groupId: String,
        deviceName: String?,
        contract: LocationSharingContract?,
        notifyOthers: Boolean,
        enforceSafetySignature: Boolean
    ) {
        startProcessing(groupId)

        try {
            triggerRemoteOnly {
                // refresh all group data before sharing starts
                logg.tag("sharing_start").d("Start location register, refresh group data")
                val result1 =
                    groupPreRefreshUseCase.refreshGroupWithMembers(groupId, throwOnError = true)
                logg.tag("sharing_start").d("Group refreshed ($result1), fetch group")
                val group = groupLocal.getGroupWithMembers(groupId).firstOrNull()
                    ?: throw IllegalStateException("Group does not exist: $groupId")

                val member = groupLocal.getMemberOnce(
                    groupId = groupId,
                    userId = appPrefs.userId
                )
                    ?: throw IllegalStateException("Missing member for a group $groupId")

                val groupKey = cryptoKEKRepo.getGroupKey(groupId, member.groupKeyVersion)

                if (groupKey !is GroupMasterKey.Valid)
                    throw MissingGroupKeyException(member.groupId, member.groupKeyVersion)

                if (enforceSafetySignature) {
                    // block location sharing if safety signature is enforced
                    // this should be used for all background device registration
                    val integrity = validityVerification.checkIntegrityForDeviceKey(group = group)
                    if (integrity == IntegrityResult.CORRUPTED) {
                        throw DeviceSafetySignatureCorrupted(groupId = groupId)
                    }
                }

                // always rotate keys for every sharing session
                val keyData = deviceKeyRotation.getNewSenderKeys(
                    group = group,
                    contract = contract
                )

                val deviceData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        MemberDeviceData(
                            deviceName = deviceName ?: userMemberId.getDefaultDeviceName(groupId.asGroupOrSpaceId()),
                            deviceType = userMemberId.getCurrentDeviceType().key
                        )
                    ),
                    key = groupKey.key
                )

                val status = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        deviceStatusProvider.getStatus(
                            groupId = groupId.asGroupOrSpaceId(),
                            contract = contract
                        )
                    ),
                    key = keyData.plainTextSenderKey
                )

                val payload = LocationCreateGroupPayload(
                    groupId = groupId,
                    deviceId = appPrefs.deviceId,
                    deviceData = deviceData,
                    // TODO pass location data already
                    locationData = null,
                    status = status,
                    groupKeyVersion = groupKey.version,
                    notify = notifyOthers,
                    keys = keyData.keys
                )

                logg.tag("sharing_start").d("Send register device request")
                val response = functionsApi.runAndWait(
                    function = ServerFunction.LOCATION_CREATE_GROUP,
                    data = payload
                )

                if (response.isSuccess) {
                    logg.tag("sharing_start").d("Device registered, save integrity")

                    val userKey = userKeyRepo.activePersonalKeyWithVersion()
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
                            key = userKey.key
                        )
                    }

                    val safetySignature = getSafetySignature(groupId = groupId, member = member)

                    // always save history key entry even if invalid so we keep ratchet going
                    cryptoKEKRepo.saveGroupHistoryKey(
                        groupId = groupId,
                        userKeyVersion = userKey.version,
                        historyKeyVersion = keyData.historyKeyVersion ?: "",
                        encryptedHistoryKey = if (!keyData.plainTextHistoryKey.isNullOrBlank()) {
                            cryptoCore.encryptDataWithKey(
                                data = keyData.plainTextHistoryKey,
                                key = userKey.key
                            )
                        } else {
                            ""
                        },
                        isSynced = true
                    )

                    cryptoKEKRepo.updateIntegrity(
                        groupId = group.id,
                        integrity = groupDeviceIntegrity,
                        safetySignature = safetySignature,
                        userKeyVersion = userKey.version
                    )

                    logg.tag("sharing_start").d("Integrity updated, refresh group")
                    val result2 = groupPreRefreshUseCase.refreshGroupWithMembers(
                        groupId = groupId,
                        cacheTimeMillis = 0
                    )
                    logg.tag("sharing_start").d("Group refreshed ($result2)")
                } else {
                    logg.tag("sharing_start").w("Device not registered")
                    throw response.remoteException
                }
            }
        } finally {
            stopProcessing(groupId)
        }
    }

    private suspend fun getSafetySignature(
        groupId: String,
        member: MemberModel
    ): String {
        val publicKey = cryptoKEKRepo.getMemberPublicKey(groupId = groupId, memberUserId = appPrefs.userId)

        if (publicKey !is GroupMemberMainPublicKey.Valid) {
            throw MissingMemberKeyException(groupId = groupId, memberId = member.id)
        }

        return GroupIntegrity.createDeviceSafetySignature(
            hashProvider = cryptoCore.hashProvider,
            groupId = groupId,
            memberId = member.id,
            memberVerification = member.verification,
            identityKey = publicKey.identityKey.key,
            membershipKey = publicKey.membershipKey,
        )
    }
}