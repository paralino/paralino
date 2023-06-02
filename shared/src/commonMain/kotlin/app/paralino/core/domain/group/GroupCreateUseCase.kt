package app.paralino.core.domain.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.CreateGroupPayload
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.models.user.SimpleKeyPairData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.Constants
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class GroupCreateUseCase(
    private val userRepository: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    private val remote: GroupsRemoteSource,
    private val local: UserGroupLocalDataSource,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
) : LimitedExecutionRepository() {

    private val groupKey = "group_create"

    fun createGroup(name: String, image: ImageModel): Flow<GroupWithMembersRaw> {
        return getDataOnce(
            remote = {
                createGroupImpl(
                    name = name,
                    image = image
                ).also { cryptoKEKRepo.refreshKeysForGroup(it.group.id) }
            },
            localUpdate = { local.updateOrCreateGroup(it) }
        )
            .startProcessing(groupKey)
    }

    fun updateGroup(userGroup: UserGroup): Flow<Unit> {

        val groupId = userGroup.id

        if (isProcessing(groupId)) {
            logg.tag("group_actions").v("Processing, can't update group $groupId")
            return flow { throw IllegalStateException("Group update already in progress") }
        }

        return getDataOnce(
            remote = { remote.updateGroup(userGroup) },
            localUpdate = { local.updateGroupOnly(it) }
        )
            .startProcessing(groupId)
            .map { Unit }
    }

    private suspend fun createGroupImpl(
        name: String,
        image: ImageModel,
    ): GroupWithMembersRaw {
        return blocking {

            val user = userRepository.cachedUser!!
            val userData = user.data!!
            val myData = MemberModelData(userData.userName, userData.userImage)
            val myIdentityKeys = userKeyRepo.latestKeyPair()

            val exchangeKeyPair = cryptoCore.getNewCryptoKeyPair()

            val groupKey = cryptoCore.getNewSymmetricKey()

            val encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                data = groupKey,
                myPrivateKey = exchangeKeyPair.privateKey,
                publicKey = exchangeKeyPair.publicKey
            )

            val signedPublicKey = cryptoCore.signData(
                data = exchangeKeyPair.publicKey,
                myPrivateKey = myIdentityKeys.privateKey
            )

            val userKeys = SimpleKeyPairData(
                privateKey = exchangeKeyPair.privateKey,
                publicKey = exchangeKeyPair.publicKey
            )

            val groupData = UserGroupData(name, image, Constants.collectionGroupUser)
            val ownerVerificationSecret = cryptoCore.hashProvider.computeHash(
                data = myIdentityKeys.publicKey,
                type = HashType.SHA256
            )

            val memberVerification = cryptoCore.signData(
                data = ownerVerificationSecret,
                myPrivateKey = myIdentityKeys.privateKey,
            )

            val groupIntegrity = GroupIntegrity(
                timeUtc = nowUTC().toString(),
                signature = GroupIntegrity.createSignature(
                    hashProvider = cryptoCore.hashProvider,
                    memberVerifications = listOf(memberVerification),
                    encryptedMemberPublicKeys = listOf(signedPublicKey)
                )
            ).let {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(it),
                    key = userKeyRepo.personalKey()
                )
            }

            val payload = CreateGroupPayload(
                encryptedGroupKey = encryptedGroupKey,
                signedPublicKey = signedPublicKey,
                ownerIdentityKey = myIdentityKeys.publicKey,
                groupData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(groupData),
                    key = groupKey
                ),
                groupMemberIntegrity = groupIntegrity,
                keyId = cryptoCore.hashProvider.computeHash(
                    data = exchangeKeyPair.publicKey,
                    type = HashType.SHA256
                ),
                encryptedUserKeys = cryptoCore.encryptModel(
                    data = userKeys,
                    key = userKeyRepo.personalKey()
                ),
                memberVerification = memberVerification,
                memberData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(myData),
                    key = groupKey
                )
            )

            val data = functionsApi.runAndWait(
                remoteConfig.resourceIds.functionCreateGroupID,
                BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (data.isSuccess) {
                remote.getGroupWithMembers(data.response)
            } else {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to create group",
                    Exception(data.stderr)
                )
            }
        }
    }

}