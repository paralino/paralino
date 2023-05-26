package app.paralino.core.domain.crypto

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.domain.LimitedExecutionState
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.raw.GenericFunctionPayload
import app.paralino.core.models.raw.GroupSenderKeyPayload
import app.paralino.core.models.raw.MemberLocationDataPayload
import app.paralino.core.models.raw.UpdateGroupMemberSenderKeysPayload
import app.paralino.core.models.user.SimpleKeyPairData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.extensions.safeLet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString

class MemberDeviceKeyRotation(
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val userRepository: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepository: CryptoKEKRepository,
    private val validityVerification: GroupValidityVerification,
    private val userMemberIdUseCase: UserMemberIdUseCase
) : ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<LimitedExecutionState<String>>())

    suspend fun rotateDeviceKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false
    ): Boolean {

        // if group is not verified (current member is not verified) skip key generation
        if (!remoteGroup.isGroupVerified) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .v("Group ${remoteGroup.id} not verified")
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
            return false
        }

        val unverifiedMembersRotation = shouldRemoveMemberKeyFromUnverifiedMembers(remoteGroup)

        if (unverifiedMembersRotation) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .w("Group ${remoteGroup.id} has unverified members with user member key, force rotation")
        }

        if (force || deviceIntegrity == IntegrityResult.VALID_EXPIRED || unverifiedMembersRotation) {
            logg.tag("tag_crypto_key_group_device_rotation")
                .w("group member key is deprecated - rotating: ${remoteGroup.id}")

            if (isProcessing(remoteGroup.id)) {
                logg.tag("tag_crypto_key_group_device_rotation")
                    .v("Group ${remoteGroup.id} device key rotation already in progress")
                return false
            }

            setAsProcessing(remoteGroup.id)

            try {
                rotateDeviceKeysImpl(remoteGroup)
            } finally {
                stopProcessing(remoteGroup.id)
            }

            return true
        } else {
            logg.tag("tag_crypto_key_group_device_rotation")
                .w("not rotating device keys for group ${remoteGroup.id}")
            return false
        }
    }

    private suspend fun rotateDeviceKeysImpl(
        group: UserGroupWithMembers
    ) {

        val groupKey =
            cryptoKEKRepository.getGroupKey(group.id, group.ownerUserId, keyVersion = null)

        if (groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(group.id)

        val currentDeviceLocation =
            userMemberIdUseCase.getCurrentLocationForGroup(group.id) ?: return

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

        val payload = GenericFunctionPayload(
            route = "rotate-device-keys",
            payload = payloadData
        )

        val response = functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionUpdateGroupMemberKeysID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )

        if (!response.isSuccess) {
            throw BaseRemoteException.fromCode(
                null,
                "Unable to rotate device keys",
                Exception(response.stderr)
            )
        } else {
            val groupDeviceIntegrity = GroupIntegrity(
                timeUtc = nowUTC().toString(),
                signature = GroupIntegrity.createSignature(
                    hashProvider = cryptoCore.hashProvider,
                    memberVerifications = group.members.map { it.member.verification },
                    groupKey = groupKey.key,
                    encryptedMemberPublicKeys = cryptoKEKRepository.getAllEncryptedMembershipPublicKeys(
                        group.id
                    )
                )
            ).let {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(it),
                    key = userKeyRepo.personalKey
                )
            }
            cryptoKEKRepository.updateIntegrity(
                locationId = newLocationData.locationId,
                integrity = groupDeviceIntegrity
            )
        }
    }

    private suspend fun getNewLocationData(
        group: UserGroupWithMembers,
        groupKey: GroupMasterKey.Valid,
        currentDeviceLocation: MemberLocation?,
        forceSenderKeyCreation: Boolean = false
    ): MemberLocationDataPayload {

        // rotate current keys if location is available
        // keys will be rotated anyway when location sharing restarts

        val locationDataValid = when (currentDeviceLocation?.locationData) {
            null,
            CryptoResource.MissingKey,
            CryptoResource.NotInitialized,
            is CryptoResource.Ready -> true

            is CryptoResource.UnverifiedKeyOrData,
            CryptoResource.WrongKeyOrData -> false
        }

        val senderKeys = if (locationDataValid || forceSenderKeyCreation) {
            getNewSenderKeys(group)
        } else {
            null to emptyList()
        }

        val newLocationData = safeLet(
            p1 = currentDeviceLocation?.locationData?.data,
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

        return MemberLocationDataPayload(
            locationId = currentDeviceLocation?.id ?: "",
            deviceId = config.deviceId,
            groupKeyVersion = groupKey.version,
            deviceData = currentDeviceLocation?.deviceData?.data?.let {
                cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(it),
                    groupKey.key
                )
            } ?: "",
            locationData = newLocationData ?: "",
            encryptedSenderKeys = senderKeys.second
        )
    }

    suspend fun getNewSenderKeys(group: UserGroupWithMembers): Pair<String, List<GroupSenderKeyPayload>> {

        val myMembershipKeys = cryptoKEKRepository.getMyPreKeyForGroup(group.id, version = 0)

        if (myMembershipKeys !is UserGroupPersonalKeyPair.Valid)
            throw MissingMemberKeyException(group.id, config.userId)

        val newSenderKey = cryptoCore.getNewSymmetricKey()

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
                    memberKeyVersion = memberPublicKey.version
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