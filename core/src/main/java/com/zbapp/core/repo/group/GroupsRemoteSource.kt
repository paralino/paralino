package com.zbapp.core.repo.group

import com.zbapp.core.config
import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.group.CryptoKEKRepository
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.GroupMapperCore
import com.zbapp.core.models.raw.*
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.base.asImageModel
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.*
import com.zbapp.core.utils.exceptions.*
import com.zbapp.core.utils.extensions.blocking
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
                collectionId = remoteConfig.resourceIds.collectionGroupsID,
                docId = mapped.id,
                data = mapped.toPostJson()
            ).parseGroup()
        }
    }

    suspend fun addUserToGroup(
        groupId: String,
        codeData: PersonalCodeReadData,
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
                publicKey = codeData.ownerKey
            )

            val encryptedMemberKey = cryptoCore.encryptDataWithKeyPair(
                data = memberKey.data.content,
                // sign with owner private key
                myPrivateKey = myKey.privateKey,
                // encrypt with user public key
                publicKey = codeData.ownerKey
            )

            val encryptedOldKeys = oldKeys.map {
                GroupMemberOldKeyPayload(
                    version = it.version,
                    encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                        data = it.data.content,
                        // sign with owner private key
                        myPrivateKey = myKey.privateKey,
                        // encrypt with user public key
                        publicKey = codeData.ownerKey
                    )
                )
            }

            val payload = CreateGroupMemberPayload(
                groupId = groupId,
                userId = codeData.ownerId,
                userPublicKey = codeData.ownerKey,
                encryptedGroupKey = encryptedGroupKey,
                encryptedOwnerKey = encryptedMemberKey,
                oldKeys = encryptedOldKeys,
                memberData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        MemberModelData(codeData.ownerName, codeData.ownerImageBase64?.asImageModel())
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
            val groups = api.getDocuments(remoteConfig.resourceIds.collectionGroupsID).map { it.parseGroup() }
            val members = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersID).map { it.parseGroupMember() }
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
            val group = api.getDocument(remoteConfig.resourceIds.collectionGroupsID, groupId).parseGroup()
            val members = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersID, "groupId" to groupId).map { it.parseGroupMember() }
            GroupWithMemberRaw(
                group = group,
                members = members
            )
        }
    }

    suspend fun getGroupOnly(groupId: String): GroupDocumentRaw {
        return blocking {
            api.getDocument(remoteConfig.resourceIds.collectionGroupsID, groupId).parseGroup()
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