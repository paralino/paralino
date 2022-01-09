package com.zbapp.core.repo.group

import com.zbapp.core.config
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.mappers.IMemberMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.ConflictException
import com.zbapp.core.utils.PermissionDeniedException
import com.zbapp.core.utils.blocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val remoteConfig: BackendConfig,
    private val mapper: IGroupDataMapperCore,
    private val memberMapper: IMemberMapperCore
) {

    suspend fun createGroup(group: UserGroup, me: UserMe): GroupDocumentRaw {
        return blocking {
            val mapped = mapper.core2Remote(group)

            val doc = api.createOrUpdateDocument(
                collectionId = remoteConfig.collectionGroupsID,
                data = mapped.copy(members = emptyList()).toPostJson(),
                docId = mapped.id
            ).let {
                BaseRemoteModule.formatter.decodeFromString<GroupDocumentRaw>(it)
            }

            val member = MemberModel(
                id = "",
                groupId = doc.id,
                userId = me.id,
                data = MemberModelData(me.name, me.data?.imageBase64),
                mutableData = null
            )
            addUserToGroup(doc, member)
        }
    }

    suspend fun updateGroup(group: UserGroup): GroupDocumentRaw {
        return blocking {
            val mapped = mapper.core2Remote(group)
            api.createOrUpdateDocument(
                remoteConfig.collectionGroupsID,
                mapped.toPostJson(),
                mapped.id
            ).let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    suspend fun addUserToGroup(group: UserGroup, vararg member: MemberModel): GroupDocumentRaw {
        return addUserToGroup(group.id, group.members.map { it.userId }, *member)
    }

    suspend fun addUserToGroup(
        group: GroupDocumentRaw,
        vararg member: MemberModel
    ): GroupDocumentRaw {
        return addUserToGroup(group.id, group.members.map { it.ownerId }, *member)
    }

    suspend fun addUserToGroup(
        groupId: String,
        currentMembers: List<String>,
        vararg member: MemberModel
    ): GroupDocumentRaw {
        return blocking {

            member.forEach {
                val mapped = memberMapper.core2Remote(it)
                api.createChildDocument(
                    collectionId = remoteConfig.collectionLocationsID,
                    data = mapped.toPostJson(),
                    read = null,
                    write = null,
                    parentDocId = groupId,
                    parentDocProperty = "members",
                    appendToParent = true
                )
            }

            api.createOrUpdateDocument(
                collectionId = remoteConfig.collectionGroupsID,
                data = JsonObject(emptyMap()),
                docId = groupId,
                read = currentMembers
                    .toMutableList()
                    .apply { addAll(member.map { it.userId }) }
                    .filter { it != config.userId }
                    .map { "user:$it" }
                    .ifEmpty { null },
                write = null
            ).let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    suspend fun removeUserFromGroup(
        group: UserGroup,
        member: MemberModel
    ): GroupDocumentRaw {
        return blocking {

            if(member.id == group.ownerId)
                throw PermissionDeniedException("Owner can not be removed from the group")

            api.deleteDocument(remoteConfig.collectionLocationsID, member.id)
            api.createOrUpdateDocument(
                collectionId = remoteConfig.collectionGroupsID,
                data = JsonObject(emptyMap()),
                docId = group.id,
                read = group.members.map { it.userId }
                    .filter { it != member.userId }
                    .map { "user:$it" }
                    .ifEmpty { null },
                write = null
            ).let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    suspend fun getAllGroups(): List<GroupDocumentRaw> {
        return blocking {
            api.getDocuments(remoteConfig.collectionGroupsID).let {
                BaseRemoteModule.formatter.parseToJsonElement(it).jsonObject["documents"]?.jsonArray?.let {
                    BaseRemoteModule.formatter.decodeFromJsonElement(it)
                } ?: emptyList()
            }
        }
    }

    suspend fun getGroup(groupId: String): GroupDocumentRaw {
        return blocking {
            api.getDocument(remoteConfig.collectionGroupsID, groupId).let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    suspend fun deleteGroup(group: UserGroup) {
        blocking {
            api.deleteDocument(remoteConfig.collectionGroupsID, group.id)
            group.members.forEach {
                api.deleteDocument(remoteConfig.collectionLocationsID, it.id)
            }
        }
    }

}