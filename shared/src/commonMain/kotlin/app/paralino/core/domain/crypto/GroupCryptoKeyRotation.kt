package app.paralino.core.domain.crypto

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.raw.GroupKeysPayload
import app.paralino.core.models.raw.GroupPlaceDataUpdatePayload
import app.paralino.core.models.raw.UpdateGroupKeysPayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString

class GroupCryptoKeyRotation constructor(
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val placesRepo: PlacesRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val userRepository: UserRepository,
    private val cryptoKEKRepository: CryptoKEKRepository,
    private val memberCryptoKeyRotation: MemberCryptoKeyRotation,
    private val deviceKeyRotation: MemberDeviceKeyRotation,
    private val validityVerification: GroupValidityVerification
) : LimitedExecution() {

    suspend fun rotateGroupAndMemberKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean
    ): Boolean {
        return if (rotateGroupAndMemberKeysIfNeeded(remoteGroup, force)) {
            true
        } else {
            val didRotateMemberKey = memberCryptoKeyRotation.rotateMemberKeys(remoteGroup, force = false)
            if(didRotateMemberKey)
                cryptoKEKRepository.refreshMemberKeysForGroup(groupId = remoteGroup.id)

            val didRotateDeviceKeys = deviceKeyRotation.rotateDeviceKeys(remoteGroup, force = false)

            didRotateMemberKey || didRotateDeviceKeys
        }
    }

    private suspend fun rotateGroupAndMemberKeysIfNeeded(
        remoteGroup: UserGroupWithMembers,
        force: Boolean
    ): Boolean {
        if (remoteGroup.ownerUserId != userRepository.cachedUserId) return false
        // if group is not verified (current member is not verified - group owner) skip key generation
        if (!remoteGroup.isGroupVerified) {
            logg.tag("tag_crypto_key_group_rotation").v("Group ${remoteGroup.id} not verified")
            return false
        }

        try {
            startProcessing(remoteGroup.id)
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_rotation")
                .v("Group ${remoteGroup.id} key rotation already in progress (${e::class.simpleName})")
            return false
        }

        val integrity = validityVerification.checkIntegrityForGroupKey(remoteGroup)
        logg.tag("tag_crypto_key_group_rotation").v("Group ${remoteGroup.id} integrity: $integrity")
        if (integrity == IntegrityResult.CORRUPTED) {
            logg.tag("tag_crypto_key_group_rotation")
                .w("Group ${remoteGroup.id} integrity corrupted, skip key rotation")
            stopProcessing(remoteGroup.id)
            return false
        }

        if (integrity == IntegrityResult.VALID && !force) {
            logg.tag("tag_crypto_key_group_rotation")
                .v("No need to rotate group ${remoteGroup.id} keys")
            stopProcessing(remoteGroup.id)
            return false
        }

        logg.tag("tag_crypto_key_group_rotation")
            .w("group key is deprecated - rotating: ${remoteGroup.id}")

        // this will rotate and refresh both group and member keys
        try {
            rotateGroupAndMemberKeys(remoteGroup)
        } finally {
            stopProcessing(remoteGroup.id)
        }

        return true
    }

    private suspend fun rotateGroupAndMemberKeys(
        remoteGroup: UserGroupWithMembers
    ) {

        val places = placesRepo.getPlacesForGroupLocally(remoteGroup.id).first()

        // 1. rotate group key
        rotateGroupKeys(group = remoteGroup, places = places)
        // new group key is required first since we are re-encrypting user data with it
        cryptoKEKRepository.refreshGroupKeysForGroup(groupId = remoteGroup.id)

        // 2. only rotate member keys
        val didRotateMemberKeys =
            memberCryptoKeyRotation.rotateMemberKeys(remoteGroup = remoteGroup, force = true)
        if (didRotateMemberKeys)
            cryptoKEKRepository.refreshMemberKeysForGroup(groupId = remoteGroup.id)

        // 3. rotate current device keys
        val didRotateDeviceKeys = deviceKeyRotation.rotateDeviceKeys(remoteGroup = remoteGroup, force = true)
        if (didRotateDeviceKeys)
            cryptoKEKRepository.refreshDeviceKeysForGroup(groupId = remoteGroup.id)
    }

    private suspend fun rotateGroupKeys(
        group: UserGroupWithMembers,
        places: List<GroupPlace>
    ) {
        blocking {

            val myMembershipKeys = cryptoKEKRepository.getMyPreKeyForGroup(group.id, version = 0)

            if (myMembershipKeys !is UserGroupPersonalKeyPair.Valid)
                throw MissingMemberKeyException(group.id, config.userId)

            val newGroupKey = cryptoCore.getNewSymmetricKey()

            val groupData = group.data.data?.let {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(it),
                    key = newGroupKey
                )
            } ?: ""

            val placesData = places.mapNotNull { place ->
                place.data.data?.let {
                    GroupPlaceDataUpdatePayload(
                        placeId = place.id,
                        placeData = cryptoCore.encryptDataWithKey(
                            data = BaseRemoteModule.formatter.encodeToString(place.data.data),
                            key = newGroupKey
                        )
                    )
                }
            }

            val usedKeys = group.members.map { it.member.groupKeyVersion } +
                    group.members.flatMap { it.locations.map { it.groupKeyVersion } } +
                    places.map { it.groupKeyVersion } +
                    group.groupKeyVersion

            val keys = group.members.mapNotNull { groupMember ->

                if (!groupMember.member.isVerified)
                    return@mapNotNull null

                val memberPublicKey =
                    cryptoKEKRepository.getMemberPublicKey(group.id, groupMember.member.userId)
                if (memberPublicKey is GroupMemberMainPublicKey.Valid) {
                    GroupKeysPayload(
                        userId = groupMember.member.userId,
                        encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                            data = newGroupKey,
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

            val groupIntegrity = GroupIntegrity(
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
                    key = userKeyRepo.personalKey()
                )
            }

            val payload = UpdateGroupKeysPayload(
                groupId = group.id,
                integrity = groupIntegrity,
                keyVersionsToKeep = usedKeys.distinct(),
                encryptedGroupKeys = keys,
                groupKeyVersion = usedKeys.max().plus(1),
                groupData = groupData,
                places = placesData
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.KEYS_UPDATE_GROUP,
                data = payload
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to update group keys",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

}