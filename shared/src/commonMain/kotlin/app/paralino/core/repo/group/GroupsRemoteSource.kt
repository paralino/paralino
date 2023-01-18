package app.paralino.core.repo.group

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.*
import app.paralino.core.models.user.AsymmetricCryptoKeyData
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
import app.paralino.core.utils.extensions.fromJsonObjectToMapOfPrimitives
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
        myData: MemberModelData,
        myKey: AsymmetricCryptoKeyData
    ): GroupWithMemberRaw {
        return blocking {

            val groupKey = cryptoCore.getNewSymmetricKey().content
            val memberSenderKey = cryptoCore.getNewSymmetricKey().content

            val encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                data = groupKey,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )

            val encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                data = memberSenderKey,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )

            val encryptedPublicKey = cryptoCore.encryptDataWithKeyPair(
                data = myKey.publicKey,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )


            val groupData = UserGroupData(name, image, Constants.collectionGroupUser)

            val payload = CreateGroupPayload(
                encryptedGroupKey = encryptedGroupKey,
                encryptedMemberKey = encryptedSenderKey,
                encryptedPublicKey = encryptedPublicKey,
                groupData = cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(groupData),
                    groupKey
                ),
                memberData = cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(myData),
                    groupKey
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
                val response = data.response.fromJsonObjectToMapOfPrimitives()
                    .filterValues { it != null }
                    .mapValues { it.value!! }
                response.parseGroup()
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
        return blocking {

            val groupId = group.id
            val groupKey = cryptoKEKRepo.getGroupKey(groupId, keyVersion = null)
                ?: throw MissingGroupKeyException(groupId)

            val memberKey = cryptoKEKRepo.getMemberKey(groupId, config.userId)
                ?: throw MissingMemberKeyException(groupId, config.userId)

            val encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                data = groupKey.data.content,
                // sign with owner private key
                myPrivateKey = myKey.privateKey,
                // encrypt with user public key
                publicKey = codeData.ownerPublicKey
            )

            val encryptedMemberKey = cryptoCore.encryptDataWithKeyPair(
                data = memberKey.data.content,
                // sign with owner private key
                myPrivateKey = myKey.privateKey,
                // encrypt with user public key
                publicKey = codeData.ownerPublicKey
            )

            val oldKeys = cryptoKEKRepo.getAllGroupKeys(groupId)
            val encryptedOldKeys = oldKeys
                .distinctBy { it.version }
                .map { key ->
                    GroupMemberOldKeyPayload(
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
                val key = cryptoKEKRepo.getMemberPublicKey(groupId, groupMember.member.userId)
                if(key != null) {
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
                if(key != null) {
                    val encryptedKey = cryptoCore.encryptDataWithKey(
                        data = codeData.ownerPublicKey,
                        key = key
                    )
                    GroupMemberPublicKeyPayload(
                        userId = codeData.ownerId,
                        encryptedPublicKey = encryptedKey
                    )
                } else {
                    null
                }
            }


            val payload = CreateGroupMemberPayload(
                groupId = groupId,
                userId = codeData.ownerId,
                encryptedGroupKey = encryptedGroupKey,
                encryptedOwnerKey = encryptedMemberKey,
                oldKeys = encryptedOldKeys,
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
                getGroupWithMembers(groupId)
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
                                locations = locations.filter { location -> member.locationIds.contains(location.id) }
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
                        locations = locations.filter { location -> member.locationIds.contains(location.id) }
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