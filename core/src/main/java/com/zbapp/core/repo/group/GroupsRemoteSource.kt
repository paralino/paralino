package com.zbapp.core.repo.group

import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.GroupDataMapperCore
import com.zbapp.core.models.raw.*
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
    private val cryptoAuth: CryptoAuth
) {

    suspend fun createGroup(group: UserGroup, me: UserMe): GroupWithMemberRaw {
        return blocking {

            val groupKey = cryptoAuth.getNewSymmetricKey()
            val encryptedGroupKey = cryptoAuth.encryptDataWithKeyPair(
                data = groupKey,
                myPrivateKey = me.data?.latestAsymmetricKey?.privateKey!!,
                publicKey = me.data.latestAsymmetricKey?.publicKey!!
            )

            val memberSenderKey = cryptoAuth.getNewSymmetricKey()
            val encryptedSenderKey = cryptoAuth.encryptDataWithKeyPair(
                data = memberSenderKey,
                myPrivateKey = me.data.latestAsymmetricKey?.privateKey!!,
                publicKey = me.data.latestAsymmetricKey?.publicKey!!
            )

            val groupData = UserGroupData(group.name, group.emoji, Constants.collectionGroupUser)
            val memberData = MemberModelData(me.name, me.data.imageBase64)

            val payload = CreateGroupPostData(
                userPublicKey = me.data.latestAsymmetricKey?.publicKey!!,
                encryptedGroupKey = encryptedGroupKey,
                encryptedMemberKey = encryptedSenderKey,
                groupData = cryptoAuth.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(groupData),
                    groupKey
                ),
                memberData = cryptoAuth.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(memberData),
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
        group: UserGroup,
        member: MemberModel,
        owner: UserMe
    ): GroupWithMemberRaw {
        return blocking {
            val encryptedGroupKey = cryptoAuth.encryptDataWithKeyPair(
                data = group.groupKey,
                myPrivateKey = owner.data?.latestAsymmetricKey?.privateKey!!,
                publicKey = member.publicKey
            )

            val payload = CreateGroupMemberFunctionData(
                groupId = group.id,
                userId = member.userId,
                userPublicKey = member.publicKey,
                encryptedGroupKey = encryptedGroupKey,
                memberData = cryptoAuth.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(member.data),
                    group.groupKey
                )
            )

            val data = functionsApi.runAndWait(
                remoteConfig.resourceIds.functionCreateGroupMemberID,
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
            val groupKeys = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupKeysID).map { it.parseGroupKey() }
            val members = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersID).map { it.parseGroupMember() }
            val groupMemberKeys = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersKeysID).map { it.parseGroupMemberKey() }
            groups.map { group ->
                GroupWithMemberRaw(
                    group = group,
                    groupKey = groupKeys.first { it.groupId == group.id },
                    members = members,
                    memberKeys = groupMemberKeys
                )
            }
        }
    }

    suspend fun getGroup(groupId: String): GroupWithMemberRaw {
        return blocking {
            val groupKey = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupKeysID, "groupId" to groupId).takeIf { it.size == 1 }!!.map { it.parseGroupKey() }.first()
            val group = api.getDocument(remoteConfig.resourceIds.collectionGroupsID, groupId).parseGroup()
            val members = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersID, "groupId" to groupId).map { it.parseGroupMember() }
            val groupMemberKeys = api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersKeysID, "groupId" to groupId).map { it.parseGroupMemberKey() }
            GroupWithMemberRaw(
                group = group,
                groupKey = groupKey,
                members = members,
                memberKeys = groupMemberKeys
            )
        }
    }

    suspend fun deleteGroup(group: UserGroup) {
        blocking {
            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionDeleteGroupID,
                data = group.id
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

    private suspend fun Map<String, Any>.parseGroup(): GroupDocumentRaw {
        return GroupDocumentRaw.from(this)
    }

    private suspend fun Map<String, Any>.parseGroupMember(): MemberDocumentRaw {
        return MemberDocumentRaw.from(this)
    }

    private suspend fun Map<String, Any>.parseGroupKey(): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw.from(this)
    }

    private suspend fun Map<String, Any>.parseGroupMemberKey(): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw.from(this)
    }

}