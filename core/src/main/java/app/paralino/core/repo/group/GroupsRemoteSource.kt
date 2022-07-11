package app.paralino.core.repo.group

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.*
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.*
import app.paralino.core.utils.exceptions.*
import app.paralino.core.utils.extensions.blocking
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


            val groupData = UserGroupData(name, image, Constants.collectionGroupUser)

            val payload = CreateGroupPayload(
                userPublicKey = myKey.publicKey,
                encryptedGroupKey = encryptedGroupKey,
                encryptedMemberKey = encryptedSenderKey,
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
            api.createOrUpdateDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupsID,
                docId = mapped.id,
                data = mapped.toPostJson()
            ).parseGroup()
        }
    }

    suspend fun addUserToGroup(
        groupId: String,
        codeData: UserPersonalCodeData,
        myKey: AsymmetricCryptoKeyData
    ): GroupWithMemberRaw {
        return blocking {

            val groupKey = cryptoKEKRepo.getLatestGroupKey(groupId)
                ?: throw MissingGroupKeyException(groupId)

            val memberKey = cryptoKEKRepo.getMemberKey(groupId, config.userId)
                ?: throw MissingMemberKeyException(groupId, config.userId)

            val oldKeys = cryptoKEKRepo.getAllGroupKeys(groupId)

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

            val encryptedOldKeys = oldKeys.map {
                GroupMemberOldKeyPayload(
                    version = it.version,
                    encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                        data = it.data.content,
                        // sign with owner private key
                        myPrivateKey = myKey.privateKey,
                        // encrypt with user public key
                        publicKey = codeData.ownerPublicKey
                    )
                )
            }

            val payload = CreateGroupMemberPayload(
                groupId = groupId,
                userId = codeData.ownerId,
                userPublicKey = codeData.ownerPublicKey,
                encryptedGroupKey = encryptedGroupKey,
                encryptedOwnerKey = encryptedMemberKey,
                oldKeys = encryptedOldKeys,
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
            val groups = api.getDocuments(databaseType = BackendDatabaseType.DATA, remoteConfig.resourceIds.collectionGroupsID).map { it.parseGroup() }
            val members = api.getDocumentsFor(databaseType = BackendDatabaseType.DATA, remoteConfig.resourceIds.collectionGroupMembersID).map { it.parseGroupMember() }
            groups.map { group ->
                GroupWithMemberRaw(
                    group = group,
                    members = members.filter { it.groupId == group.id }
                )
            }
        }
    }

    suspend fun getGroupWithMembers(groupId: String): GroupWithMemberRaw {
        return blocking {
            val group = api.getDocument(databaseType = BackendDatabaseType.DATA, remoteConfig.resourceIds.collectionGroupsID, groupId).parseGroup()
            val members = api.getDocumentsFor(databaseType = BackendDatabaseType.DATA, remoteConfig.resourceIds.collectionGroupMembersID, "groupId" to groupId).map { it.parseGroupMember() }
            GroupWithMemberRaw(
                group = group,
                members = members
            )
        }
    }

    suspend fun getGroupOnly(groupId: String): GroupDocumentRaw {
        return blocking {
            api.getDocument(databaseType = BackendDatabaseType.DATA, remoteConfig.resourceIds.collectionGroupsID, groupId).parseGroup()
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

}