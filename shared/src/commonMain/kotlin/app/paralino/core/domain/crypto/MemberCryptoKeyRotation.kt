package app.paralino.core.domain.crypto

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.DecryptedGroupKeyData
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.raw.GroupMemberDataPayload
import app.paralino.core.models.raw.GroupMemberKeyPayload
import app.paralino.core.models.raw.MemberLocationDataPayload
import app.paralino.core.models.raw.UpdateGroupMemberKeysPayload
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString

class MemberCryptoKeyRotation(
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val userRepository: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepository: CryptoKEKRepository,
    private val validityVerification: GroupValidityVerification
): ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())


    suspend fun rotateMemberKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false,
        refresh: Boolean = true,
    ): Boolean {

        val integrity = validityVerification.checkIntegrity(remoteGroup)
        if(integrity == IntegrityResult.CORRUPTED)
            return false

        if (force || integrity == IntegrityResult.VALID_EXPIRED) {
            logg.tag("tag_crypto_key_group_member")
                .w("group member key is deprecated - rotating: ${remoteGroup.id}")

            if (isProcessing(remoteGroup.id)) {
                return false
            }

            setAsProcessing(remoteGroup.id)
            val myKey = userKeyRepo.latestKeyPair()
            val groupKey = cryptoKEKRepository.getGroupKey(remoteGroup.id, remoteGroup.ownerUserId)!!
            try {
                rotateMemberKeysForGroup(remoteGroup, groupKey, myKey)

                if(refresh)
                    cryptoKEKRepository.refreshKeysForGroup(remoteGroup.id).first()

                stopProcessing(remoteGroup.id)
            } catch (e: Exception) {
                stopProcessing(remoteGroup.id)
                throw e
            }

            return true
        } else {
            logg.tag("tag_crypto_key_group_member")
                .w("not rotating member keys, create missing instead: ${remoteGroup.id}")
            createMissingMemberKeys(remoteGroup)
            return false
        }
    }

    private suspend fun rotateMemberKeysForGroup(
        group: UserGroupWithMembers,
        groupKey: DecryptedGroupKeyData,
        myKey: AsymmetricCryptoKeyData
    ) {
        blocking {

            val newSenderKey = cryptoCore.getNewSymmetricKey().content

            // update data for every user device
            // this way any device from user that gets to update the keys will update data for all other devices
            // no need for other devices to do the same since encryption key is unique per user
            val member = group.members.filter { it.member.userId == config.userId }.firstOrNull()!!

            val groupIntegrity = GroupIntegrity(
                timeUtc = nowUTC().toString(),
                signature = GroupIntegrity.createSignature(
                    hashProvider = cryptoCore.hashProvider,
                    memberVerifications = group.members.map { it.member.verification },
                    groupKey = groupKey.data.asBase64String(cryptoCore.hashProvider),
                    encryptedMemberPublicKeys = cryptoKEKRepository.getAllEncryptedMemberPublicKeys(group.id)
                )
            ).let {
                cryptoCore.encryptDataWithKeyPair(
                    data = BaseRemoteModule.formatter.encodeToString(it),
                    myPrivateKey = myKey.privateKey,
                    publicKey = myKey.publicKey
                )
            }

            // TODO review:
            // if group key is somehow removed, this crashes because we can not decrypt the data
            // what is happening now is that remote data is re-encrypted
            // but since this data is user own data, we can replace it if remote is missing or wrong
            val memberData = GroupMemberDataPayload(
                groupKeyVersion = groupKey.version,
                integrity = groupIntegrity,
                memberData = cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(member.member.memberData.data),
                    groupKey.data.content
                ),
                locations = member.locations.map { location ->
                    MemberLocationDataPayload(
                        locationId = location.id,
                        deviceData = location.deviceData.data?.let {
                            cryptoCore.encryptDataWithKey(
                                BaseRemoteModule.formatter.encodeToString(it),
                                groupKey.data.content
                            )
                        } ?: "",
                        locationData = location.locationData.data?.let {
                            cryptoCore.encryptDataWithKey(
                                BaseRemoteModule.formatter.encodeToString(it),
                                newSenderKey
                            )
                        } ?: ""
                    )
                }
            )

            val keys = group.members.mapNotNull { groupMember ->

                if(!groupMember.member.isVerified)
                    return@mapNotNull null

                val memberPublicKey = cryptoKEKRepository.getMemberPublicKey(group.id, groupMember.member.userId)
                if(memberPublicKey != null) {
                    GroupMemberKeyPayload(
                        userId = groupMember.member.userId,
                        encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                            data = newSenderKey,
                            // sign with owner private key
                            myPrivateKey = myKey.privateKey,
                            // encrypt with user public key
                            publicKey = memberPublicKey
                        ),
                        keyVersion = null
                    )
                } else {
                    null
                }
            }

            val payload = UpdateGroupMemberKeysPayload(
                groupId = group.id,
                deleteOldKeys = true,
                encryptedSenderKeys = keys,
                memberData = memberData
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUpdateGroupMemberKeysID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to update group member keys",
                    Exception(response.stderr)
                )
            }
        }
    }

    private suspend fun createMissingMemberKeys(remoteGroup: UserGroupWithMembers) {

        // get ids of members that already have key created by current user
        val memberKeysCreatedByMe = cryptoKEKRepository.getMemberKeys().filter {
            it.groupId == remoteGroup.id && it.ownerUserId == userRepository.cachedUserId
        }

        // get all other members from group that are missing key
        val membersThatNeedKeyUpdate = remoteGroup.members.filter { member ->
            val memberKey = memberKeysCreatedByMe.firstOrNull { it.memberUserId == member.member.userId }
            member.member.isVerified && memberKey != null && memberKey.encryptedSenderKey == null
        }

        if (membersThatNeedKeyUpdate.isNotEmpty()) {
            logg.tag("tag_crypto_key_group_member")
                .d("Creating missing member keys for users (${membersThatNeedKeyUpdate.map { it.member.userId }}) in group ${remoteGroup.id}")
            val userKey = userKeyRepo.latestKeyPair()
            val myMemberKey =
                cryptoKEKRepository.getMemberKey(
                    groupId = remoteGroup.id,
                    memberUserId = userRepository.cachedUserId
                )
            createMemberKeysForSpecificMembers(
                groupId = remoteGroup.id,
                members = membersThatNeedKeyUpdate.map { it.member },
                myKey = userKey,
                // if member key is null, it will be created in next step
                // this means that this user has no keys yet like if it just added to a group
                currentSenderKey = myMemberKey
            )
            cryptoKEKRepository.refreshMemberKeysForGroup(groupId = remoteGroup.id)
        } else {
            logg.tag("tag_crypto_key_group_member")
                .d("No missing member keys for group ${remoteGroup.id}")
        }
    }

    private suspend fun createMemberKeysForSpecificMembers(
        groupId: String,
        members: List<MemberModel>,
        myKey: AsymmetricCryptoKeyData,
        currentSenderKey: DecryptedGroupKeyData?
    ) {
        blocking {

            // if no key is passed, create new one
            val newSenderKey =
                currentSenderKey?.data?.content ?: cryptoCore.getNewSymmetricKey().content

            val keys = members.mapNotNull { groupMember ->

                if(!groupMember.isVerified)
                    return@mapNotNull null

                val memberPublicKey = cryptoKEKRepository.getMemberPublicKey(groupId, groupMember.userId)
                if(memberPublicKey != null) {
                    GroupMemberKeyPayload(
                        userId = groupMember.userId,
                        encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                            data = newSenderKey,
                            // sign with owner private key
                            myPrivateKey = myKey.privateKey,
                            // encrypt with user public key
                            publicKey = memberPublicKey
                        ),
                        keyVersion = currentSenderKey?.version
                    )
                } else {
                    null
                }
            }

            val payload = UpdateGroupMemberKeysPayload(
                groupId = groupId,
                deleteOldKeys = false,
                encryptedSenderKeys = keys,
                // no data passed here since we are only creating keys for new members
                // keys didn't change so no need to re-encrypt the data
                memberData = null
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUpdateGroupMemberKeysID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to update missing group member keys",
                    Exception(response.stderr)
                )
            }
        }
    }

}