package app.paralino.core.domain.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.location.MemberVerification
import app.paralino.core.models.raw.CreateGroupPayload
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.Constants
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.*
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
) : BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())
    private val groupKey = "group_create"

    fun createGroup(name: String, image: ImageModel): Flow<GroupWithMembersRaw> {

        if (isProcessing(groupKey)) {
            logg.tag("group_actions").v("Processing, can't create new group $groupKey")
            return flow { throw IllegalStateException("Group creation already in progress") }
        }

        setAsProcessing(groupKey)
        return getDataOnce(
            remote = {
                createGroupImpl(
                    name = name,
                    image = image
                )
                    .also { cryptoKEKRepo.refreshKeysForGroup(it.group.id).first() }
                    .also { stopProcessing(groupKey) }
            },
            localUpdate = { local.updateOrCreateGroup(it) }
        )
            .catch {
                stopProcessing(groupKey)
                throw it
            }
    }


    private suspend fun createGroupImpl(
        name: String,
        image: ImageModel,
    ): GroupWithMembersRaw {
        return blocking {

            val user = userRepository.cachedUser!!
            val userData = user.data!!
            val myData = MemberModelData(userData.userName, userData.userImage)
            val myKey = userKeyRepo.latestKeyPair()

            val secretKey = cryptoCore.getNewSymmetricKey()
            val secretHash = cryptoCore.hashProvider
                .computeHash(secretKey.content, HashType.SHA256)
                .asBase64String(cryptoCore.hashProvider)

            val secretKeyForVerification = cryptoCore.signData(
                data = secretHash,
                myPrivateKey = myKey.privateKey
            )
            val secretKeyForMe = cryptoCore.encryptDataWithKey(
                data = secretKey.asBase64String(cryptoCore.hashProvider),
                key = userKeyRepo.personalKey.content
            )

            val groupKey = cryptoCore.getNewSymmetricKey()
            val memberSenderKey = cryptoCore.getNewSymmetricKey()

            val encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                data = groupKey.content,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )

            val encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                data = memberSenderKey.content,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )

            val encryptedPublicKey = cryptoCore.encryptDataWithKey(
                data = myKey.publicKey,
                key = myKey.publicKey
            )

            val groupData = UserGroupData(name, image, Constants.collectionGroupUser)
            val ownerVerificationSecret = cryptoCore.hashProvider.computeHash(
                data = cryptoCore.keyProvider.createNewSymmetricKey().content,
                type = HashType.SHA256
            ).asBase64String(cryptoCore.hashProvider)

            val memberVerification = MemberVerification(
                groupOwnerSecret = ownerVerificationSecret,
                memberSecret = secretKeyForVerification
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
                    memberVerifications = listOf(memberVerification),
                    groupKey = groupKey.asBase64String(cryptoCore.hashProvider),
                    encryptedMemberPublicKeys = listOf(encryptedPublicKey)
                )
            )

            val payload = CreateGroupPayload(
                encryptedGroupKey = encryptedGroupKey,
                encryptedMemberKey = encryptedSenderKey,
                encryptedPublicKey = encryptedPublicKey,
                groupData = cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(groupData),
                    groupKey.content
                ),
                groupMemberIntegrity = cryptoCore.encryptDataWithKeyPair(
                    data = BaseRemoteModule.formatter.encodeToString(groupIntegrity),
                    myPrivateKey = myKey.privateKey,
                    publicKey = myKey.publicKey
                ),
                secretKey = secretKeyForMe,
                memberVerification = memberVerification,
                memberData = cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(myData),
                    groupKey.content
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