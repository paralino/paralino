package app.paralino.core.domain.crypto

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.location.MemberDeviceStatusProvider
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.raw.GroupSenderKeyPayload
import app.paralino.core.models.raw.MemberLocationDataPayload
import app.paralino.core.models.raw.UpdateGroupMemberSenderKeysPayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.extensions.safeLet
import kotlinx.serialization.encodeToString

class MemberDeviceKeyRotation(
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val userRepository: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepository: CryptoKEKRepository,
    private val validityVerification: GroupValidityVerification,
    private val deviceStatusProvider: MemberDeviceStatusProvider
) : LimitedExecution() {

    suspend fun rotateDeviceKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false
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

        val shouldRotate = force || deviceIntegrity.shouldChange || unverifiedMembersRotation

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
            locationData = newLocationData,
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.KEYS_UPDATE_LOCATIONS,
            data = payloadData
        )

        if (!response.isSuccess) {
            throw BaseRemoteException.fromRemote(
                code = response.statusCode,
                error = response.errorResponse
            )
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
            cryptoKEKRepository.updateIntegrity(
                groupId = group.id,
                integrity = groupDeviceIntegrity,
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
    ): MemberLocationDataPayload {

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
            getNewSenderKeys(group)
        } else {
            null to emptyList()
        }

        val newLocationData = safeLet(
            p1 = currentDeviceLocation.locationData.data,
            p2 = senderKeys.first,
            block = { locationData, key ->
                suspend {
                    cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(locationData),
                        key = key
                    )
                }
            }
        )?.invoke()

        val status = senderKeys.first?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    deviceStatusProvider.getStatus(group.id)
                ),
                key = it
            )
        }

        return MemberLocationDataPayload(
            locationId = currentDeviceLocation.id,
            deviceId = config.deviceId,
            groupKeyVersion = groupKey.version,
            deviceData = currentDeviceLocation.deviceData.data?.let {
                cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(it),
                    groupKey.key
                )
            } ?: "",
            locationData = newLocationData ?: "",
            status = status ?: "",
            encryptedSenderKeys = senderKeys.second
        )
    }

    suspend fun getNewSenderKeys(group: UserGroupWithMembers): Pair<String, List<GroupSenderKeyPayload>> {

        val myMembershipKeys = cryptoKEKRepository.getMyPreKeyForGroup(group.id, version = 0)

        if (myMembershipKeys !is UserGroupPersonalKeyPair.Valid)
            throw MissingMemberKeyException(group.id, config.userId)

        val newSenderKey = cryptoCore.getNewSymmetricKeyForLocationData()

        val keys = group.members.mapNotNull { groupMember ->

            if (!groupMember.member.isVerified) {
                logg.tag("tag_crypto_key_group_device_rotation")
                    .d("Member ${groupMember.member.userId} not verified, skip key creation")
                return@mapNotNull null
            }

            val memberPublicKey =
                cryptoKEKRepository.getMemberPublicKey(group.id, groupMember.member.userId)
            if (memberPublicKey is GroupMemberMainPublicKey.Valid) {
                GroupSenderKeyPayload(
                    userId = groupMember.member.userId,
                    encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                        data = newSenderKey,
                        // sign with owner private key
                        myPrivateKey = myMembershipKeys.key.privateKey,
                        // encrypt with user public key
                        publicKey = memberPublicKey.key
                    ),
                    memberKeyVersion = memberPublicKey.memberKeyVersion
                )
            } else {
                null
            }
        }

        return newSenderKey to keys
    }

    private suspend fun shouldRemoveMemberKeyFromUnverifiedMembers(remoteGroup: UserGroupWithMembers): Boolean {
        // get ids of members that already have key created by current user
        val memberKeysCreatedByMe = cryptoKEKRepository.getGroupDeviceKeys().filter {
            it.groupId == remoteGroup.id && it.ownerUserId == userRepository.cachedUserId
        }

        // check if any unverified member has key
        return remoteGroup.members.any { member ->
            val memberKey = memberKeysCreatedByMe.firstOrNull {
                it.memberUserId == member.member.userId
            }
            !member.member.isVerified && memberKey != null && !memberKey.encryptedSenderKey.isNullOrBlank()
        }
    }
}