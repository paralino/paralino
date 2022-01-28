package com.zbapp.core.repo.group

import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.crypto.CryptoKeyRepository
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.invite.PersonalCodeReadData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.GroupDataMapperCore
import com.zbapp.core.models.raw.*
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.*
import kotlinx.serialization.encodeToString

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val mapper: GroupDataMapperCore,
    private val cryptoAuth: CryptoAuth,
    private val cryptoKeyRepo: CryptoKeyRepository
) {

    suspend fun createGroup(
        name: String,
        image: String,
        myData: MemberModelData,
        myKey: AsymmetricCryptoKeyData
    ): GroupWithMemberRaw {
        return blocking {

            val groupKey = cryptoAuth.getNewSymmetricKey()
            val encryptedGroupKey = cryptoAuth.encryptDataWithKeyPair(
                data = groupKey,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )

            val memberSenderKey = cryptoAuth.getNewSymmetricKey()
            val encryptedSenderKey = cryptoAuth.encryptDataWithKeyPair(
                data = memberSenderKey,
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )

            val groupData = UserGroupData(name, image, Constants.collectionGroupUser)

            val payload = CreateGroupPostData(
                userPublicKey = myKey.publicKey,
                encryptedGroupKey = encryptedGroupKey,
                encryptedMemberKey = encryptedSenderKey,
                groupData = cryptoAuth.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(groupData),
                    groupKey
                ),
                memberData = cryptoAuth.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(myData),
                    groupKey
                )
            )

            val data = functionsApi.runAndWait(
                remoteConfig.resourceIds.functionCreateGroupID,
                BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (data.isSuccess) {
                getGroup(data.stdout)
            } else {
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

            val groupKey = cryptoKeyRepo.getGroupKey(groupId)
                ?: throw NotFoundException("Group key not found")

            val encryptedGroupKey = cryptoAuth.encryptDataWithKeyPair(
                data = groupKey,
                myPrivateKey = myKey.privateKey,
                publicKey = codeData.ownerKey
            )

            val payload = CreateGroupMemberFunctionData(
                groupId = groupId,
                userId = codeData.ownerId,
                userPublicKey = codeData.ownerKey,
                encryptedGroupKey = encryptedGroupKey,
                memberData = cryptoAuth.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(
                        MemberModelData(codeData.ownerName, codeData.ownerImageBase64)
                    ),
                    key = groupKey
                )
            )

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionCreateGroupMemberID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (data.isSuccess) {
                getGroup(data.stdout)
            } else {
                throw BaseRemoteException.fromCode(
                    code = null,
                    message = "Unable to create group",
                    cause = Exception(data.stderr)
                )
            }
        }
    }

    suspend fun removeUserFromGroup(group: UserGroup, member: MemberModel) {
        return blocking {

            if (member.userId == group.ownerUserId)
                throw PermissionDeniedException("Owner can not be removed from the group")

            val payload = DeleteGroupMemberFunctionData(
                groupId = group.id,
                userId = member.userId
            )

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionDeleteGroupMemberID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!data.isSuccess) {
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
                    members = members
                )
            }
        }
    }

    suspend fun getGroup(groupId: String): GroupWithMemberRaw {
        return blocking {
            val group = api.getDocument(remoteConfig.resourceIds.collectionGroupsID, groupId).parseGroup()
            val members = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersID, "groupId" to groupId).map { it.parseGroupMember() }
            GroupWithMemberRaw(
                group = group,
                members = members
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