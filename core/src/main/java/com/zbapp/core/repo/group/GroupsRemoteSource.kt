package com.zbapp.core.repo.group

import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.raw.CreateGroupMemberFunctionData
import com.zbapp.core.models.raw.CreateGroupPostData
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val mapper: IGroupDataMapperCore,
    private val cryptoAuth: CryptoAuth
) {

    suspend fun createGroup(group: UserGroup, me: UserMe): GroupDocumentRaw {
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
                api.getDocument(
                    collectionId = remoteConfig.resourceIds.collectionGroupsID,
                    documentId = data.stdout
                ).parseGroup()
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
    ): GroupDocumentRaw {
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
                api.getDocument(
                    collectionId = remoteConfig.resourceIds.collectionGroupsID,
                    documentId = data.stdout
                ).parseGroup()
            } else {
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to create group",
                    Exception(data.stderr)
                )
            }
        }
    }

    suspend fun removeUserFromGroup(
        group: UserGroup,
        member: MemberModel
    ): GroupDocumentRaw {
        return blocking {

            if (member.id == group.ownerId)
                throw PermissionDeniedException("Owner can not be removed from the group")

            api.deleteDocument(remoteConfig.resourceIds.collectionGroupMembersID, member.id)
            api.createOrUpdateDocument(
                collectionId = remoteConfig.resourceIds.collectionGroupsID,
                docId = group.id,
                data = JsonObject(emptyMap()),
                read = group.members.map { it.userId }
                    .filter { it != member.userId }
                    .map { "user:$it" }
                    .ifEmpty { null },
                write = null
            ).parseGroup()
        }
    }

    suspend fun getAllGroups(): List<GroupDocumentRaw> {
        return blocking {
            api.getDocuments(remoteConfig.resourceIds.collectionGroupsID).map { it.parseGroup() }
        }
    }

    suspend fun getGroup(groupId: String): GroupDocumentRaw {
        return blocking {
            api.getDocument(remoteConfig.resourceIds.collectionGroupsID, groupId).parseGroup()
        }
    }

    suspend fun deleteGroup(group: UserGroup) {
        blocking {
            api.deleteDocument(remoteConfig.resourceIds.collectionGroupsID, group.id)
            group.members.forEach {
                api.deleteDocument(remoteConfig.resourceIds.collectionGroupMembersID, it.id)
            }
        }
    }

    private suspend fun Map<String, Any>.parseGroup(): GroupDocumentRaw {
        return GroupDocumentRaw.from(this)
    }

}