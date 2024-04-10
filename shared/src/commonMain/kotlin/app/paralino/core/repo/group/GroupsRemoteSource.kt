package app.paralino.core.repo.group

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.BlockGroupFunctionData
import app.paralino.core.models.raw.BlockGroupMemberFunctionData
import app.paralino.core.models.raw.DeleteGroupMemberPayload
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.models.raw.UpdateGroupPayload
import app.paralino.core.models.user.BlockListModelData
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.PermissionDeniedException
import app.paralino.core.utils.extensions.blocking

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val mapper: GroupMapperCore,
    private val cryptoCore: CryptoCore,
    private val cryptoUserKeyRepo: CryptoUserKeyRepository
) {

    suspend fun updateGroup(group: UserGroup): GroupDocumentRaw {
        return blocking {
            val mapped = mapper.core2Remote(group)

            val payload = UpdateGroupPayload(
                groupId = group.id,
                groupData = mapped.data
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.GROUP_UPDATE,
                data = payload
            )

            if (response.isSuccess) {
                response.responseDataMap!!.parseGroup()
            } else {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to update group",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

    suspend fun removeUserFromGroup(groupId: String, groupOwnerId: String, memberUserId: String) {
        return blocking {

            if (memberUserId == groupOwnerId)
                throw PermissionDeniedException("Owner can not be removed from the group")

            val payload = DeleteGroupMemberPayload(
                groupId = groupId,
                userId = memberUserId
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.MEMBER_DELETE,
                data = payload
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to delete group member",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

    suspend fun getAllGroups(): List<GroupWithMembersRaw> {
        return blocking {
            val groups = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupsID
            ).map { it.parseGroup() }

            val members = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersID
            ).map { it.parseGroupMember() }

            val settings = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupSettingsID
            ).map { it.parseGroupSettings() }

            val locations = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
            ).map { it.parseMemberLocation() }

            groups.map { group ->
                GroupWithMembersRaw(
                    group = group,
                    members = members
                        .filter { it.groupId == group.id }
                        .map { member ->
                            MemberWithLocationsRaw(
                                member = member.copy(settingsRaw = settings.firstOrNull { it.id == member.id && it.groupId == group.id }),
                                locations = locations.filter { location ->
                                    member.locationIds.contains(location.id)
                                }
                            )
                        }
                )
            }
        }
    }

    suspend fun getGroupWithMembers(groupId: String): GroupWithMembersRaw {
        val group = api.getDocument(
            databaseType = BackendDatabaseType.DATA,
            remoteConfig.resourceIds.collectionGroupsID,
            groupId
        ).parseGroup()

        val members = api.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionGroupMembersID,
            queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
        ).map { it.parseGroupMember() }

        val settings = api.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionGroupSettingsID,
            queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
        ).map { it.parseGroupSettings() }
            .takeIf { it.size == 1 && it.first().ownerUserId == config.userId }
            ?.firstOrNull()

        val locations = api.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
            queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
        ).map { it.parseMemberLocation() }

        return GroupWithMembersRaw(
            group = group,
            members = members.map { member ->
                MemberWithLocationsRaw(
                    member = member.copy(settingsRaw = settings?.takeIf { it.id == member.id }),
                    locations = locations.filter { location ->
                        member.locationIds.contains(location.id)
                    }
                )
            }
        )
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            val response = functionsApi.runAndWait(
                function = ServerFunction.GROUP_DELETE,
                data = groupId
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to delete group",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

    suspend fun blockGroup(group: UserGroup) {
        return blocking {

            if (config.userId == group.ownerUserId)
                throw PermissionDeniedException("Owner can not block the group")

            val groupData = group.data.data?.let {
                cryptoCore.encryptModel(
                    data = BlockListModelData(
                        name = it.name,
                        image = it.image
                    ),
                    key = cryptoUserKeyRepo.personalKey()
                )
            } ?: ""

            val payload = BlockGroupFunctionData(
                groupId = group.id,
                groupData = groupData
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.GROUP_BLOCK,
                data = payload
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to block group",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

    suspend fun blockUser(user: MemberModel) {
        return blocking {

            if (config.userId == user.userId)
                throw PermissionDeniedException("You can not block yourself")

            val userData = user.memberData.data?.let {
                cryptoCore.encryptModel(
                    data = BlockListModelData(
                        name = it.name,
                        image = it.image ?: ImageModel.emoji(Emojis.robot)
                    ),
                    key = cryptoUserKeyRepo.personalKey()
                )
            } ?: ""

            val payload = BlockGroupMemberFunctionData(
                groupId = user.groupId,
                userId = user.userId,
                userData = userData
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.MEMBER_BLOCK,
                data = payload
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to block user",
                    cause = Exception(response.stderr)
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

    private fun Map<String, Any>.parseGroupSettings(): GroupSettingsRaw {
        return GroupSettingsRaw.from(this)
    }

    private fun Map<String, Any>.parseMemberLocation(): MemberLocationRaw {
        return MemberLocationRaw.from(this)
    }
}