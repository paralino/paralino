package app.paralino.core.domain.group

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.location.MemberModelParalinoIdData
import app.paralino.core.models.raw.CreateGroupMemberPayload
import app.paralino.core.models.raw.GroupMemberGroupKeysData
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class GroupMemberAddUseCase constructor(
    private val local: UserGroupLocalDataSource,
    private val groupRefreshUseCase: GroupRefreshUseCase,
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
) : LimitedExecutionRepository() {

    fun addMemberByAccessCode(
        groupId: String,
        code: UserPersonalCodeData
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val group = local.getGroupWithMembers(groupId).firstOrNull()
                    ?: throw IllegalStateException("Group does not exist: $groupId")

                addUserToGroup(
                    group = group,
                    codeData = code
                )
                groupRefreshUseCase.refreshRemoteGroupWithMembersAndPlaces(groupId, cacheTimeMillis = 0)
            }
        )
            .map { Unit }
            .startProcessing(groupId)
    }


    private suspend fun addUserToGroup(
        group: UserGroupWithMembers,
        codeData: UserPersonalCodeData
    ) {

        if (!group.isGroupVerified)
            throw IllegalStateException("Group is unverified")

        val groupId = group.id
        val groupKey = cryptoKEKRepo.getGroupKey(groupId, keyVersion = null)

        if (groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(groupId)

        val memberPublicKey = cryptoCore.verifySignature(
            data = codeData.keyData.signedOwnerKxPublicKey,
            publicKey = codeData.userData.ownerIdentityKey
        ) ?: throw CryptoException("Wrong member key signature")

        val myKey = userKeyRepo.latestKeyPair()
        val myMembershipKeys = cryptoKEKRepo.getMyPreKeyForGroup(groupId, version = 0)

        if (myMembershipKeys !is UserGroupPersonalKeyPair.Valid)
            throw MissingMemberKeyException(groupId, config.userId)

        val allGroupKeys = cryptoKEKRepo.getAllGroupKeys(groupId)
        val encryptedOldKeys = allGroupKeys
            .distinctBy { it.version }
            .map { key ->
                GroupMemberGroupKeysData(
                    version = key.version,
                    encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                        data = key.key,
                        // sign with owner private exchange key
                        myPrivateKey = myMembershipKeys.key.privateKey,
                        // encrypt with user public key
                        publicKey = memberPublicKey
                    )
                )
            }

        val ownerVerificationSecret = cryptoCore.hashProvider.computeHash(
            data = codeData.userData.ownerIdentityKey,
            type = HashType.GENERIC_256
        )

        val memberVerification = cryptoCore.signData(
            data = ownerVerificationSecret,
            myPrivateKey = myKey.privateKey,
        )

        val groupIntegrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            signature = GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                serverIntegrity = cryptoCore.hashProvider.computeHash(
                    data = group.group.serverIntegrity.plus(codeData.userData.ownerUserId),
                    type = HashType.GENERIC_256
                ),
                memberVerifications = group.members.map { it.member.verification } + memberVerification,
                encryptedMemberPublicKeys = cryptoKEKRepo.getAllEncryptedMembershipPublicKeys(
                    groupId = groupId
                ) + codeData.keyData.signedOwnerKxPublicKey
            )
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKeyRepo.personalKey()
            )
        }

        val payload = CreateGroupMemberPayload(
            groupId = groupId,
            codeKeyId = codeData.codeKeyId,
            keyId = codeData.keyData.keyId,
            groupOwnerIntegrity = groupIntegrity,
            memberVerification = memberVerification,
            userToAddId = codeData.userData.ownerUserId,
            groupKeys = encryptedOldKeys,
            memberSignedPublicKey = codeData.keyData.signedOwnerKxPublicKey,
            memberIdentityKey = codeData.userData.ownerIdentityKey,
            serverIntegrity = cryptoCore.hashProvider.computeHash(
                data = group.group.serverIntegrity.plus(codeData.userData.ownerUserId),
                type = HashType.GENERIC_256
            ),
            memberData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelData(codeData.userData.ownerName, codeData.userData.ownerImage)
                ),
                key = groupKey.key
            ),
            paralinoId = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelParalinoIdData(codeData.keyData.code)
                ),
                key = groupKey.key
            )
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.MEMBER_CREATE,
            data = payload
        )

        if (!response.isSuccess) {
            throw response.remoteException
        }
    }
}