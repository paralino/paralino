package com.zbapp.core.repo.group

import com.zbapp.core.config
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.mappers.IMemberMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.blocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

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
                data = mapped.copy(members = emptyList()).toPostString(),
                docId = mapped.id
            ).let {
                BaseRemoteModule.formatter.decodeFromString<GroupDocumentRaw>(it)
            }

            val member = MemberModel(
                id = "",
                groupId = doc.id,
                userId = me.id,
                imageBase64 = me.data?.imageBase64,
                name = me.name,
                data = null
            )
            addUserToGroup(doc, member)
        }
    }

    suspend fun updateGroup(group: UserGroup): GroupDocumentRaw {
        return blocking {
            val mapped = mapper.core2Remote(group)
            api.createOrUpdateDocument(
                remoteConfig.collectionGroupsID,
                mapped.toPostString(),
                mapped.id
            ).let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    suspend fun addUserToGroup(group: UserGroup, vararg member: MemberModel): GroupDocumentRaw {
        return addUserToGroup(group.id, group.members.map { it.userId }, *member)
    }

    suspend fun addUserToGroup(group: GroupDocumentRaw, vararg member: MemberModel): GroupDocumentRaw {
        return addUserToGroup(group.id, group.members.map { it.ownerId }, *member)
    }

    suspend fun addUserToGroup(groupId: String, currentMembers: List<String>, vararg member: MemberModel): GroupDocumentRaw {
        return blocking {

            member.forEach {
                val mapped = memberMapper.core2Remote(it)
                api.createChildDocument(
                    collectionId = remoteConfig.collectionLocationsID,
                    data = mapped.toPostString(),
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
        group: GroupDocumentRaw,
        member: MemberDocumentRaw
    ): GroupDocumentRaw {
        return blocking {
            api.deleteDocument(remoteConfig.collectionLocationsID, member.id)
            api.createOrUpdateDocument(
                collectionId = remoteConfig.collectionGroupsID,
                data = JsonObject(emptyMap()),
                docId = group.id,
                read = group.members.map { it.ownerId }
                    .filter { it != config.userId && it != member.ownerId }
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