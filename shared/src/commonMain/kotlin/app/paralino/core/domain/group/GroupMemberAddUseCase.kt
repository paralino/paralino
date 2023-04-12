package app.paralino.core.domain.group

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.domain.LimitedExecutionState
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.location.MemberVerification
import app.paralino.core.models.raw.*
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString

class GroupMemberAddUseCase constructor(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
): BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<LimitedExecutionState<String>>())

    fun addMemberByAccessCode(groupId: String, code: UserPersonalCodeData): Flow<GroupDocumentRaw> {

        if (isProcessing(groupId)) {
            logg.tag("group_actions").v("Processing, can't add new member $groupId")
            return flow { throw IllegalStateException("Group member creation already in progress") }
        }

        return getDataOnce(
            remote = {
                setAsProcessing(groupId)
                val group = local.getGroupWithMembers(groupId).first()
                addUserToGroup(
                    group,
                    code,
                    userKeyRepo.latestKeyPair()
                ).also {
                    cryptoKEKRepo.refreshKeysForGroup(groupId).first()
                }.also {
                    stopProcessing(groupId)
                }
            },
            localUpdate = {
                local.updateOrCreateGroup(it)
            }
        )
            .map { it.group }
            .catch {
                stopProcessing(groupId)
                throw it
            }
    }


    suspend fun addUserToGroup(
        group: UserGroupWithMembers,
        codeData: UserPersonalCodeData,
        myKey: AsymmetricCryptoKeyData
    ): GroupWithMembersRaw {

        if(!group.isGroupVerified)
            throw IllegalStateException("Group is unverified")

        val groupId = group.id
        val groupKey = cryptoKEKRepo.getGroupKey(groupId, keyVersion = null)
            ?: throw MissingGroupKeyException(groupId)

        val memberKey = cryptoKEKRepo.getMemberKey(groupId, config.userId)
            ?: throw MissingMemberKeyException(groupId, config.userId)

        val encryptedMemberKey = cryptoCore.encryptDataWithKeyPair(
            data = memberKey.data.content,
            // sign with owner private key
            myPrivateKey = myKey.privateKey,
            // encrypt with user public key
            publicKey = codeData.ownerPublicKey
        )

        val allGroupKeys = cryptoKEKRepo.getAllGroupKeys(groupId)
        val encryptedOldKeys = allGroupKeys
            .distinctBy { it.version }
            .map { key ->
                GroupMemberGroupKeysData(
                    version = key.version,
                    encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                        data = key.data.content,
                        // sign with owner private key
                        myPrivateKey = myKey.privateKey,
                        // encrypt with user public key
                        publicKey = codeData.ownerPublicKey
                    )
                )
            }

        val newUserOwnPublicKey = GroupMemberPublicKeyPayload(
            userId = codeData.ownerId,
            encryptedPublicKey = cryptoCore.encryptDataWithKey(
                data = codeData.ownerPublicKey,
                key = codeData.ownerPublicKey
            )
        )

        val keysForNewUser = group.members.mapNotNull { groupMember ->

            if (!groupMember.member.isVerified)
                return@mapNotNull null

            val key = cryptoKEKRepo.getMemberPublicKey(groupId, groupMember.member.userId)
            if (key != null) {
                val encryptedKey = cryptoCore.encryptDataWithKey(
                    data = key,
                    key = codeData.ownerPublicKey
                )
                GroupMemberPublicKeyPayload(
                    userId = groupMember.member.userId,
                    encryptedPublicKey = encryptedKey
                )
            } else {
                null
            }
        } + newUserOwnPublicKey

        val userKeyForOtherUsers = group.members.mapNotNull { groupMember ->
            val key = cryptoKEKRepo.getMemberPublicKey(groupId, groupMember.member.userId)
            if (key != null) {
                val encryptedKey = cryptoCore.encryptDataWithKey(
                    data = codeData.ownerPublicKey,
                    key = key
                )
                GroupMemberPublicKeyPayload(
                    userId = groupMember.member.userId,
                    encryptedPublicKey = encryptedKey
                )
            } else {
                null
            }
        }

        val keyId = cryptoCore.derivePassword(codeData.code).loginPassword

        // just to provide randomness from group owner
        val ownerVerificationSecret = cryptoCore.hashProvider.computeHash(
            data = cryptoCore.getNewSymmetricKey().content,
            type = HashType.SHA256
        ).asBase64String(cryptoCore.hashProvider)
//            .let {
//                cryptoCore.encryptDataWithKeyPair(
//                    data = it,
//                    myPrivateKey = myKey.privateKey,
//                    publicKey = myKey.publicKey
//                )
//            }.let {
//                cryptoCore.signData(
//                    data = it,
//                    myPrivateKey = myKey.privateKey,
//                )
//            }

        val memberVerification = MemberVerification(
            groupOwnerSecret = ownerVerificationSecret,
            memberSecret = codeData.secretKey
        ).let {
            BaseRemoteModule.formatter.encodeToString(it)
        }.let {
            cryptoCore.signData(
                data = it,
                myPrivateKey = myKey.privateKey,
            )
        }

        val groupIntegrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            signature = GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                memberVerifications = group.members.map { it.member.verification } + memberVerification,
                groupKey = groupKey.data.asBase64String(cryptoCore.hashProvider),
                encryptedMemberPublicKeys = cryptoKEKRepo.getAllEncryptedMemberPublicKeys(groupId) + newUserOwnPublicKey.encryptedPublicKey
            )
        ).let {
            cryptoCore.encryptDataWithKeyPair(
                data = BaseRemoteModule.formatter.encodeToString(it),
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )
        }

        val payload = CreateGroupMemberPayload(
            groupId = groupId,
            keyId = keyId,
            groupMemberIntegrity = codeData.integrity,
            groupOwnerIntegrity = groupIntegrity,
            memberVerification = memberVerification,
            userToAddId = codeData.ownerId,
            encryptedOwnerKey = encryptedMemberKey,
            groupKeys = encryptedOldKeys,
            keysForNewUser = keysForNewUser,
            userKeyForOtherUsers = userKeyForOtherUsers,
            memberData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelData(codeData.ownerName, codeData.ownerImage)
                ),
                key = groupKey.data.content
            )
        )

        val data = functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionCreateGroupMemberID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )

        if (data.isSuccess) {
            return remote.getGroupWithMembers(groupId)
        } else {
            // TODO throw better exception
            throw BaseRemoteException.fromCode(
                code = null,
                message = "Unable to create group",
                cause = Exception(data.stderr)
            )
        }
    }

}