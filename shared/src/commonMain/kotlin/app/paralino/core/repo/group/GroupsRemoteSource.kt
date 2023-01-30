package app.paralino.core.repo.group

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.location.MemberVerification
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.*
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.Constants
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.exceptions.PermissionDeniedException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.encodeToString

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val mapper: GroupMapperCore,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository
) {

    suspend fun createGroup(
        name: String,
        image: ImageModel,
        code: UserPersonalCode,
        myData: MemberModelData,
        myKey: AsymmetricCryptoKeyData
    ): GroupWithMemberRaw {
        return blocking {

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
            ).asBase64String(cryptoCore.hashProvider).let {
                cryptoCore.encryptDataWithKeyPair(
                    data = it,
                    myPrivateKey = myKey.privateKey,
                    publicKey = myKey.publicKey
                )
            }.let {
                cryptoCore.signData(
                    data = it,
                    myPrivateKey = myKey.privateKey,
                )
            }

            val memberVerification = MemberVerification(
                groupOwnerSecret = ownerVerificationSecret,
                memberSecret = code.data.secretKey
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
                groupOwnIntegrity = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(groupIntegrity),
                    key = groupKey.content,
                ),
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
                getGroupWithMembers(data.response)
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

    suspend fun updateGroup(group: UserGroup): GroupDocumentRaw {
        return blocking {
            val mapped = mapper.core2Remote(group)

            val payload = UpdateGroupPayload(
                groupId = group.id,
                groupData = mapped.data
            )

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUpdateGroupID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (data.isSuccess) {
                data.responseMap.parseGroup()
            } else {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = null,
                    message = "Unable to update group",
                    cause = Exception(data.stderr)
                )
            }
        }
    }

    suspend fun addUserToGroup(
        group: UserGroupWithMembers,
        codeData: UserPersonalCodeData,
        myKey: AsymmetricCryptoKeyData
    ): GroupWithMemberRaw {

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

            if(!groupMember.member.isVerified)
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
            data = cryptoCore.keyProvider.createNewSymmetricKey().content,
            type = HashType.SHA256
        ).asBase64String(cryptoCore.hashProvider).let {
            cryptoCore.encryptDataWithKeyPair(
                data = it,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )
        }.let {
            cryptoCore.signData(
                data = it,
                myPrivateKey = myKey.privateKey,
            )
        }

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
                encryptedMemberPublicKeys = cryptoKEKRepo.getAllRawMemberPublicKeys(groupId) + newUserOwnPublicKey.encryptedPublicKey
            )
        )

        val payload = CreateGroupMemberPayload(
            groupId = groupId,
            keyId = keyId,
            groupMemberIntegrity = cryptoCore.encryptDataWithKeyPair(
                data = BaseRemoteModule.formatter.encodeToString(groupIntegrity),
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            ),
            groupOwnIntegrity = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(groupIntegrity),
                key = groupKey.data.content,
            ),
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
            return getGroupWithMembers(groupId)
        } else {
            // TODO throw better exception
            throw BaseRemoteException.fromCode(
                code = null,
                message = "Unable to create group",
                cause = Exception(data.stderr)
            )
        }
    }

    suspend fun removeUserFromGroup(group: UserGroup, memberUserId: String) {
        return blocking {

            if (memberUserId == group.ownerUserId)
                throw PermissionDeniedException("Owner can not be removed from the group")

            val payload = DeleteGroupMemberPayload(
                groupId = group.id,
                userId = memberUserId
            )

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionDeleteGroupMemberID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!data.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to delete group member",
                    Exception(data.stderr)
                )
            }
        }
    }

    suspend fun getAllGroups(): List<GroupWithMemberRaw> {
        return blocking {
            val groups = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupsID
            ).map { it.parseGroup() }

            val members = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersID
            ).map { it.parseGroupMember() }

            val locations = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
            ).map { it.parseMemberLocation() }

            groups.map { group ->
                GroupWithMemberRaw(
                    group = group,
                    members = members
                        .filter { it.groupId == group.id }
                        .map { member ->
                            MemberWithLocationsRaw(
                                member = member,
                                locations = locations.filter { location ->
                                    member.locationIds.contains(location.id)
                                }
                            )
                        }
                )
            }
        }
    }

    suspend fun getGroupWithMembers(groupId: String): GroupWithMemberRaw {
        return blocking {

            val group = api.getDocument(
                databaseType = BackendDatabaseType.DATA,
                remoteConfig.resourceIds.collectionGroupsID,
                groupId
            ).parseGroup()

            val members = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parseGroupMember() }

            val locations = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parseMemberLocation() }

            GroupWithMemberRaw(
                group = group,
                members = members.map { member ->
                    MemberWithLocationsRaw(
                        member = member,
                        locations = locations.filter { location ->
                            member.locationIds.contains(location.id)
                        }
                    )
                }
            )
        }
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionDeleteGroupID,
                data = groupId
            )

            if (!data.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to delete group",
                    Exception(data.stderr)
                )
            }
        }
    }

    private fun Map<String, Any>.parseGroup(): GroupDocumentRaw {
        return GroupDocumentRaw.from(this)
    }

    private fun Map<String, Any>.parseGroupMember(): MemberDocumentRaw {
        return MemberDocumentRaw.from(this)
    }

    private fun Map<String, Any>.parseMemberLocation(): MemberLocationRaw {
        return MemberLocationRaw.from(this)
    }

}