package app.paralino.core.domain.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.location.MemberModelParalinoIdData
import app.paralino.core.models.raw.CreateGroupPayload
import app.paralino.core.provider.isSocketTimeout
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class GroupCreateUseCase(
    private val userRepository: UserRepository,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val cryptoCore: CryptoCore,
    private val remote: GroupsRemoteSource,
    private val groupRefresh: GroupRefreshUseCase,
    private val local: UserGroupLocalDataSource,
    private val functionsApi: IFunctionsApi,
    private val personalCodesRepo: PersonalCodesRepository
) : LimitedExecutionRepository() {

    private val groupKey = "group_create"

    fun createGroup(name: String, image: ImageModel): Flow<String> {
        return getDataOnce(
            remote = {
                createGroupImpl(
                    name = name,
                    image = image
                ).also {
                    groupRefresh.refreshRemoteGroupWithMembersAndPlaces(groupId = it, cacheTimeMillis = 0)
                }
            }
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
            localUpdate = {
                local.updateGroupOnly(it)
            }
        )
            .startProcessing(groupId)
            .map { Unit }
    }

    private suspend fun createGroupImpl(
        name: String,
        image: ImageModel,
    ): String {
        val user = userRepository.cachedUser!!
        val userData = user.data.data!!
        val myData = MemberModelData(userData.userName, userData.userImage)
        val myIdentityKeys = userKeyRepo.signatureKeyPair()

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

        val groupData = UserGroupData(
            name = name,
            image = image,
            type = UserGroup.groupPrivateGroupId
        )
        val ownerVerificationSecret = cryptoCore.hashProvider.computeHash(
            data = myIdentityKeys.publicKey,
            type = HashType.GENERIC_256
        )

        val memberVerification = cryptoCore.signData(
            data = ownerVerificationSecret,
            myPrivateKey = myIdentityKeys.privateKey,
        )

        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        val groupIntegrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            signature = GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                serverIntegrity = cryptoCore.hashProvider.computeHash(
                    data = user.id,
                    type = HashType.GENERIC_256
                ),
                memberVerifications = listOf(memberVerification),
                encryptedMemberPublicKeys = listOf(signedPublicKey)
            )
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKey.key
            )
        }

        val paralinoId = personalCodesRepo.activePersonalCodeState.value?.code

        val encryptedGroupData = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(groupData),
            key = groupKey
        )

        val encryptedMemberData = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(myData),
            key = groupKey
        )

        val payload = CreateGroupPayload(
            encryptedGroupKey = encryptedGroupKey,
            signedPublicKey = signedPublicKey,
            ownerIdentityKey = myIdentityKeys.publicKey,
            groupData = encryptedGroupData,
            groupMemberIntegrity = groupIntegrity,
            userKeyVersion = userKey.version,
            serverIntegrity = cryptoCore.hashProvider.computeHash(
                data = user.id,
                type = HashType.GENERIC_256
            ),
            keyId = cryptoCore.hashProvider.computeHash(
                data = exchangeKeyPair.publicKey,
                type = HashType.GENERIC_256
            ),
            encryptedUserKeys = cryptoCore.encryptModel(
                data = userKeys,
                key = userKey.key
            ),
            memberVerification = memberVerification,
            memberData = encryptedMemberData,
            paralinoId = paralinoId?.let {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        MemberModelParalinoIdData(paralinoId)
                    ),
                    key = groupKey
                ).let {
                    cryptoCore.signData(
                        data = it,
                        myPrivateKey = myIdentityKeys.privateKey
                    )
                }
            }
        )

        val response = try {
            functionsApi.runAndWait(
                function = ServerFunction.GROUP_CREATE,
                data = payload
            )
        } catch (e: Exception) {
            if (e.isSocketTimeout()) {
                val allGroups = remote.getAllGroupsWithMembers()
                val match = allGroups.firstOrNull {
                    val memberMe = it.members.first { it.member.ownerUserId == user.id }
                    it.group.ownerUserId == user.id &&
                    it.group.data == encryptedGroupData &&
                            memberMe.member.data == encryptedMemberData &&
                            memberMe.member.settingsRaw!!.groupIntegrity == groupIntegrity
                }
                if (match != null) {
                    return match.group.id
                } else {
                    throw e
                }
            } else {
                throw e
            }
        }

        if (response.isSuccess) {
            return response.responseDataMap!!.get("groupId") as String
        } else {
            throw response.remoteException
        }
    }
}