package app.paralino.core.domain.crypto

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation.GroupDeviceSenderKeyData
import app.paralino.core.domain.location.MemberDeviceStatusProvider
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.raw.GroupSenderKeyPayload
import app.paralino.core.models.raw.MemberLocationDataPayload
import app.paralino.core.models.raw.UpdateGroupMemberSenderKeysPayload
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.extensions.safeLet

abstract class IMemberDeviceKeyRotation: LimitedExecution() {
    abstract suspend fun rotateDeviceKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false
    ): Boolean

    abstract suspend fun getNewSenderKeys(
        group: UserGroupWithMembers,
        contract: LocationSharingContract?
    ): GroupDeviceSenderKeyData
}

class NoOpMemberDeviceKeyRotation: IMemberDeviceKeyRotation() {
    override suspend fun rotateDeviceKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean
    ): Boolean {
        logg.tag("tag_crypto_key_group_device_rotation")
            .w("Group ${remoteGroup.id} NOOP operation")
        return false
    }

    override suspend fun getNewSenderKeys(
        group: UserGroupWithMembers,
        contract: LocationSharingContract?
    ): GroupDeviceSenderKeyData {
        throw IllegalStateException("Device key rotation not allowed")
    }
}

class MemberDeviceKeyRotation(
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val cryptoKEKRepository: CryptoKEKLocalRepository,
    private val validityVerification: GroupValidityVerification,
    private val deviceStatusProvider: MemberDeviceStatusProvider,
    private val locationSharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs
) : IMemberDeviceKeyRotation() {

    override suspend fun rotateDeviceKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean
    ): Boolean {

        // if group is not verified (current member is not verified) skip key generation
        if (!remoteGroup.isGroupVerified) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .w("Group ${remoteGroup.id} not verified")
            return false
        }

        if (remoteGroup.currentDeviceLocation == null) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .v("Current device doesn't have location in ${remoteGroup.id}, skip key rotation")
            return false
        }

        try {
            startProcessing(remoteGroup.id)
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .v("Group ${remoteGroup.id} device key rotation already in progress")
            return false
        }

        val memberIntegrity = validityVerification.checkIntegrityForMemberKey(remoteGroup)
        val deviceIntegrity = validityVerification.checkIntegrityForDeviceKey(remoteGroup)

        logg.tag("tag_crypto_key_group_device_rotation")
            .v("Group ${remoteGroup.id} member integrity: $memberIntegrity")
        logg.tag("tag_crypto_key_group_device_rotation")
            .v("Group ${remoteGroup.id} device integrity: $deviceIntegrity")

        if (memberIntegrity == IntegrityResult.CORRUPTED || deviceIntegrity == IntegrityResult.CORRUPTED) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .w("Group ${remoteGroup.id} integrity corrupted, skipping key rotation")
            stopProcessing(remoteGroup.id)
            return false
        }

        val unverifiedMembersRotation = shouldRemoveMemberKeyFromUnverifiedMembers(remoteGroup)

        if (unverifiedMembersRotation) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .w("Group ${remoteGroup.id} has unverified members with user member key, force rotation")
        }

        val shouldRotate = deviceIntegrity.shouldChange(
            force = force,
            isSharingActive = locationSharingManager.isSharingActiveForGroup(id = remoteGroup.id.asGroupOrSpaceId())
        ) || unverifiedMembersRotation

        if (!shouldRotate) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .d("not rotating device keys for group ${remoteGroup.id}")
            stopProcessing(remoteGroup.id)
            return false
        }

        logg.tag("tag_crypto_key_group_device_rotation")
            .i("member device key is deprecated - rotating: ${remoteGroup.id}")

        try {
            rotateDeviceKeysImpl(remoteGroup)
        } finally {
            stopProcessing(remoteGroup.id)
        }

        return true
    }

    private suspend fun rotateDeviceKeysImpl(
        group: UserGroupWithMembers
    ) {

        val currentDeviceLocation = group.currentDeviceLocation!!

        val groupKey =
            cryptoKEKRepository.getGroupKey(group.id, group.ownerUserId, keyVersion = null)

        if (groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(group.id)

        val newLocationData = getNewLocationData(
            group = group,
            groupKey = groupKey,
            currentDeviceLocation = currentDeviceLocation,
            forceSenderKeyCreation = false
        )

        val payloadData = UpdateGroupMemberSenderKeysPayload(
            groupId = group.id,
            locationData = newLocationData.locationPayload,
        )

        // Save history keys locally before upload
        // this way we have the keys regardless of result since backend only saves and distributes the keys.
        // If we would save it for successful response, there could be a case where everything is
        // actually synced properly, but device throws a network error - then we would lose history keys
        newLocationData.keyData?.keys?.mapNotNull { it.historyKey }?.let { keys ->
            cryptoKEKRepository.savePendingGroupHistoryMemberKeys(groupId = group.id, keys = keys)
        }

        val response = functionsApi.runAndWait(
            function = ServerFunction.KEYS_UPDATE_LOCATIONS,
            data = payloadData
        )

        if (!response.isSuccess) {
            response.remoteException
        } else {
            val userKey = userKeyRepo.activePersonalKeyWithVersion()
            val groupDeviceIntegrity = GroupIntegrity(
                timeUtc = nowUTC().toString(),
                signature = GroupIntegrity.createSignature(
                    hashProvider = cryptoCore.hashProvider,
                    serverIntegrity = group.group.serverIntegrity,
                    memberVerifications = group.members.map { it.member.verification },
                    encryptedMemberPublicKeys = cryptoKEKRepository.getAllEncryptedMembershipPublicKeys(
                        groupId = group.id
                    )
                )
            ).let {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(it),
                    key = userKey.key
                )
            }

            val member = group.currentUserMember!!
            val publicKey = cryptoKEKRepository.getMemberPublicKey(groupId = group.id, memberUserId = member.userId)

            if (publicKey !is GroupMemberMainPublicKey.Valid) {
                throw MissingMemberKeyException(groupId = group.id, memberId = member.id)
            }

            val safetySignature = GroupIntegrity.createDeviceSafetySignature(
                hashProvider = cryptoCore.hashProvider,
                groupId = group.id,
                memberId = member.id,
                memberVerification = member.verification,
                identityKey = publicKey.identityKey.key,
                membershipKey = publicKey.membershipKey,
            )

            if (newLocationData.keyData?.plainTextHistoryKey.isNullOrBlank() || newLocationData.keyData.historyKeyVersion.isNullOrBlank()) {
                cryptoKEKRepository.deactivateGroupHistoryKey(groupId = group.id)
            } else {
                cryptoKEKRepository.saveAndActivateGroupHistoryKey(
                    groupId = group.id,
                    userKeyVersion = userKey.version,
                    historyKeyVersion = newLocationData.keyData.historyKeyVersion,
                    encryptedHistoryKey = cryptoCore.encryptDataWithKey(
                        data = newLocationData.keyData.plainTextHistoryKey,
                        key = userKey.key
                    )
                )
            }

            cryptoKEKRepository.updateIntegrity(
                groupId = group.id,
                integrity = groupDeviceIntegrity,
                safetySignature = safetySignature,
                userKeyVersion = userKey.version
            )
            logg.tag("tag_crypto_key_group_device_rotation")
                .i("Finished rotating member device key for ${group.id}")
        }
    }

    private suspend fun getNewLocationData(
        group: UserGroupWithMembers,
        groupKey: GroupMasterKey.Valid,
        currentDeviceLocation: MemberLocation,
        forceSenderKeyCreation: Boolean = false
    ): GroupDeviceLocationKeyData {

        // rotate current keys if location is available
        // keys will be rotated anyway when location sharing restarts

        val locationDataValid = when (currentDeviceLocation.locationData) {
            // using missing key and uninitialized here because on first time sharing key nor location will be present
            CryptoResource.MissingKey,
            CryptoResource.NotInitialized,
            is CryptoResource.UnverifiedKeyOrData,
            is CryptoResource.Ready -> true

            CryptoResource.WrongKeyOrData -> false
        }

        val senderKeys = if (locationDataValid || forceSenderKeyCreation) {
            getNewSenderKeys(group = group, contract = locationSharingManager.currentSharingContractForGroup(id = group.id.asGroupOrSpaceId()))
        } else {
            null
        }

        val newLocationData = safeLet(
            p1 = currentDeviceLocation.locationData.data,
            p2 = senderKeys?.plainTextSenderKey,
            block = { locationData, key ->
                suspend {
                    cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(locationData),
                        key = key
                    )
                }
            }
        )?.invoke()

        val status = senderKeys?.plainTextSenderKey?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    deviceStatusProvider.getStatus(groupId = group.id.asGroupOrSpaceId())
                ),
                key = it
            )
        }

        val locationPayload = MemberLocationDataPayload(
            locationId = currentDeviceLocation.id,
            deviceId = appPrefs.deviceId,
            groupKeyVersion = groupKey.version,
            deviceData = currentDeviceLocation.deviceData.data?.let {
                cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(it),
                    groupKey.key
                )
            } ?: "",
            locationData = newLocationData ?: "",
            status = status ?: "",

            // location history cache always set to empty so we don't leak old history to new members
            locationHistory = "",
            encryptedSenderKeys = senderKeys?.keys?.map { it.senderKey } ?: emptyList()
        )

        return GroupDeviceLocationKeyData(
            locationPayload = locationPayload,
            keyData = senderKeys
        )
    }

    override suspend fun getNewSenderKeys(
        group: UserGroupWithMembers,
        contract: LocationSharingContract?
    ): GroupDeviceSenderKeyData {

        val myMembershipKeys = cryptoKEKRepository.getMyPreKeyForGroup(group.id, version = 0)

        if (myMembershipKeys !is UserGroupPersonalKeyPair.Valid)
            throw MissingMemberKeyException(group.id, appPrefs.userId)

        val newSenderKey = cryptoCore.getNewSymmetricKeyForLocationData()

        val (newHistoryKey, historyKeyVersion) = if (contract?.asValidContract()?.saveToHistory == true) {
            val newHistoryKey = cryptoCore.getNewSymmetricKeyForLocationData()
            val historyKeyVersion = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)
            newHistoryKey to historyKeyVersion
        } else {
            null to null
        }

        val keys = group.members.mapNotNull { groupMember ->

            if (!groupMember.member.isVerified) {
                logg.tag("tag_crypto_key_group_device_rotation")
                    .d("Member ${groupMember.member.userId} not verified, skip key creation")
                return@mapNotNull null
            }

            val memberPublicKey = cryptoKEKRepository.getMemberPublicKey(
                    groupId = group.id,
                    memberUserId = groupMember.member.userId
                )

            if (memberPublicKey is GroupMemberMainPublicKey.Valid) {
                GroupMemberKeyPayload(
                    senderKey = GroupSenderKeyPayload(
                        userId = groupMember.member.userId,
                        encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                            data = newSenderKey,
                            // sign with owner private key
                            myPrivateKey = myMembershipKeys.key.privateKey,
                            // encrypt with user public key
                            publicKey = memberPublicKey.key
                        ),
                        memberKeyVersion = memberPublicKey.memberKeyVersion
                    ),
                    historyKey = if (newHistoryKey != null && historyKeyVersion != null) {
                        GroupHistoryKeyPayload(
                            memberId = groupMember.member.id,
                            userId = groupMember.member.userId,
                            memberVerification = groupMember.member.verification,
                            memberKeyVersion = memberPublicKey.memberKeyVersion,
                            historyKeyVersion = historyKeyVersion,
                            encryptedHistoryKey = cryptoCore.encryptDataWithKeyPair(
                                data = newHistoryKey,
                                // sign with owner private key
                                myPrivateKey = myMembershipKeys.key.privateKey,
                                // encrypt with user public key
                                publicKey = memberPublicKey.key
                            )
                        )
                    } else {
                        null
                    }
                )
            } else {
                null
            }
        }

        return GroupDeviceSenderKeyData(
            plainTextSenderKey = newSenderKey,
            plainTextHistoryKey = newHistoryKey,
            historyKeyVersion = historyKeyVersion,
            keys = keys
        )
    }

    private suspend fun shouldRemoveMemberKeyFromUnverifiedMembers(remoteGroup: UserGroupWithMembers): Boolean {
        // get ids of members that already have key created by current user
        val memberKeysCreatedByMe = cryptoKEKRepository.getGroupDeviceKeys().filter {
            it.groupId == remoteGroup.id && it.ownerUserId == appPrefs.userId
        }

        // check if any unverified member has key
        return remoteGroup.members.any { member ->
            val memberKey = memberKeysCreatedByMe.firstOrNull {
                it.memberUserId == member.member.userId
            }
            !member.member.isVerified && memberKey != null && !memberKey.encryptedSenderKey.isNullOrBlank()
        }
    }

    private data class GroupDeviceLocationKeyData(
        val locationPayload: MemberLocationDataPayload,
        val keyData: GroupDeviceSenderKeyData?
    )

    data class GroupDeviceSenderKeyData(
        val plainTextSenderKey: String,
        val plainTextHistoryKey: String?,
        val historyKeyVersion: String?,
        val keys: List<GroupMemberKeyPayload>
    )

    data class GroupMemberKeyPayload constructor(
        val senderKey: GroupSenderKeyPayload,
        val historyKey: GroupHistoryKeyPayload?
    )

    data class GroupHistoryKeyPayload constructor(
        val memberId: String,
        val userId: String,
        val memberVerification: String,
        val memberKeyVersion: Long,
        val historyKeyVersion: String,
        val encryptedHistoryKey: String,
    )
}