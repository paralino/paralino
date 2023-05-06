package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.domain.LimitedExecutionState
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.raw.GroupKeysPayload
import app.paralino.core.models.raw.GroupPlaceDataUpdatePayload
import app.paralino.core.models.raw.UpdateGroupKeysPayload
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val validityVerification: GroupValidityVerification
): ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<LimitedExecutionState<String>>())

    suspend fun rotateGroupAndMemberKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean
    ): Boolean {
        return if(rotateGroupAndMemberKeysIfNeeded(remoteGroup, force)) {
           true
        } else {
            memberCryptoKeyRotation.rotateMemberKeys(remoteGroup, force = false, refresh = false)
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
        if (isProcessing(remoteGroup.id)) {
            logg.tag("tag_crypto_key_group_rotation").v("Group ${remoteGroup.id} key rotation already in progress")
            return false
        }

        val integrity = validityVerification.checkIntegrityForGroupKey(remoteGroup)
        logg.tag("tag_crypto_key_group_rotation").v("Group ${remoteGroup.id} integrity: $integrity")
        if(integrity == IntegrityResult.CORRUPTED) {
            logg.tag("tag_crypto_key_group_rotation").w("Group ${remoteGroup.id} integrity corrupted, skip key rotation")
            return false
        }

        if(integrity == IntegrityResult.VALID && !force) {
            logg.tag("tag_crypto_key_group_rotation").v("No need to rotate group ${remoteGroup.id} keys")
            return false
        }

        setAsProcessing(remoteGroup.id)

        logg.tag("tag_crypto_key_group_rotation").w("group key is deprecated - rotating: ${remoteGroup.id}")

        // this will rotate and refresh both group and member keys
        try {
            rotateGroupAndMemberKeys(remoteGroup)
            stopProcessing(remoteGroup.id)
            return true
        } catch (e: Exception) {
            stopProcessing(remoteGroup.id)
            throw e
        }
    }

    private suspend fun rotateGroupAndMemberKeys(
        remoteGroup: UserGroupWithMembers
    ) {

        val myKey = userKeyRepo.latestKeyPair()
        val places = placesRepo.getPlacesForGroupLocally(remoteGroup.id).first()

        // 1. rotate group key
        rotateGroupKeys(
            group = remoteGroup,
            places = places,
            myKey = myKey
        )

        // new group key is required first since we are re-encrypting user data with it
        // 2. fetch new key(s) for that group
        cryptoKEKRepository.refreshGroupKeysForGroup(groupId = remoteGroup.id)

        // 3. only rotate member keys
        memberCryptoKeyRotation.rotateMemberKeys(remoteGroup = remoteGroup, force = true, refresh = false)

        // 4. refresh member keys
        cryptoKEKRepository.refreshMemberKeysForGroup(groupId = remoteGroup.id)
    }

    private suspend fun rotateGroupKeys(
        group: UserGroupWithMembers,
        places: List<GroupPlace>,
        myKey: AsymmetricCryptoKeyData
    ) {
        blocking {
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
                    places.map { it.keyVersion } +
                    group.keyVersion

            val keys = group.members.mapNotNull { groupMember ->

                if(!groupMember.member.isVerified)
                    return@mapNotNull null

                val memberPublicKey = cryptoKEKRepository.getMemberPublicKey(group.id, groupMember.member.userId)
                if(memberPublicKey is GroupMemberMainPublicKey.Valid) {
                    GroupKeysPayload(
                        userId = groupMember.member.userId,
                        encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                            data = newGroupKey,
                            // sign with owner private key
                            myPrivateKey = myKey.privateKey,
                            // encrypt with user public key
                            publicKey = memberPublicKey.key
                        )
                    )
                } else {
                    null
                }
            }


            val groupIntegrity = GroupIntegrity(
                timeUtc = nowUTC().toString(),
                signature = GroupIntegrity.createSignature(
                    hashProvider = cryptoCore.hashProvider,
                    memberVerifications = group.members.map { it.member.verification },
                    groupKey = newGroupKey,
                    encryptedMemberPublicKeys = cryptoKEKRepository.getAllEncryptedMemberPublicKeys(group.id)
                )
            ).let {
                cryptoCore.encryptDataWithKeyPair(
                    data = BaseRemoteModule.formatter.encodeToString(it),
                    myPrivateKey = myKey.privateKey,
                    publicKey = myKey.publicKey
                )
            }

            val payload = UpdateGroupKeysPayload(
                groupId = group.id,
                integrity = groupIntegrity,
                keyVersionsToKeep = usedKeys.distinct(),
                encryptedGroupKeys = keys,
                groupData = groupData,
                places = placesData
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUpdateGroupKeysID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to update group keys",
                    Exception(response.stderr)
                )
            }
        }
    }

}