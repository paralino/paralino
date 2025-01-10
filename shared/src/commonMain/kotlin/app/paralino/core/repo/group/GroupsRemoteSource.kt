package app.paralino.core.repo.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.BatteryNotificationsSettingsData
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.BatteryAlertPayload
import app.paralino.core.models.raw.BatteryNotificationsUpdatePayload
import app.paralino.core.models.raw.BlockGroupFunctionData
import app.paralino.core.models.raw.BlockGroupMemberFunctionData
import app.paralino.core.models.raw.DeleteGroupMemberPayload
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.GroupWithMembersAndPlacesRaw
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
import app.paralino.core.repo.api.runAsync
import app.paralino.core.repo.places.PlacesRemoteSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.PermissionDeniedException
import app.paralino.core.utils.exceptions.UnsupportedGroupEncryptionSchemeException
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.async
import kotlinx.serialization.encodeToString

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val placesRemote: PlacesRemoteSource,
    private val appBuildConfig: AppBuildConfig,
    private val mapper: GroupMapperCore,
    private val cryptoCore: CryptoCore,
    private val cryptoUserKeyRepo: CryptoUserKeysLocalSource,
    private val appPrefs: AppPrefs
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
                throw response.remoteException
            }
        }
    }

    suspend fun updateBatteryNotifications(
        groupId: String,
        memberId: String,
        batterySettings: BatteryNotificationsSettingsData
    ): GroupSettingsRaw {
        return blocking {

            val payload = BatteryNotificationsUpdatePayload(
                groupId = groupId,
                memberId = memberId,
                alertData = BaseRemoteModule.formatter.encodeToString(batterySettings)
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.SETTINGS_NOTIFICATIONS_BATTERY_UPDATE,
                data = payload
            )

            if (response.isSuccess) {
                response.responseDataMap!!.parseGroupSettings()
            } else {
                throw response.remoteException
            }
        }
    }

    suspend fun removeUserFromGroup(groupId: String, groupOwnerId: String, memberUserId: String) {
        return blocking {

            if (memberUserId == groupOwnerId)
                throw PermissionDeniedException(code = 403, message = "Owner can not be removed from the group")

            val payload = DeleteGroupMemberPayload(
                groupId = groupId,
                userId = memberUserId
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.MEMBER_DELETE,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    suspend fun getAllGroups(): List<GroupWithMembersAndPlacesRaw> {
        return blocking {
            val groupsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionGroupsID
                ).map { it.parseGroup() }
            }

            val membersAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionGroupMembersID
                ).map { it.parseGroupMember() }
            }

            val settingsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionGroupSettingsID
                ).map { it.parseGroupSettings() }
            }

            val locationsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
                ).map { it.parseMemberLocation() }
            }

            val placesAsync = async {
                placesRemote.getAllPlaces()
            }

            val groups = groupsAsync.await()
            val members = membersAsync.await()
            val settings = settingsAsync.await()
            val locations = locationsAsync.await()
            val places = placesAsync.await()

            groups
                .filter { AppPrefs.isEncryptionSchemeSupported(it.schemeVersion) }
                .map { group ->
                    GroupWithMembersAndPlacesRaw(
                        group = GroupWithMembersRaw(
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
                                },
                        ),
                        places = places.filter { it.place.groupId == group.id }
                    )
                }
        }
    }

    suspend fun getGroupWithMembers(groupId: String): GroupWithMembersAndPlacesRaw {
        return blocking {
            val groupAsync = async {
                api.getDocument(
                    databaseType = BackendDatabaseType.DATA,
                    appBuildConfig.resourceIds.collectionGroupsID,
                    groupId
                ).parseGroup().also { group ->
                    if (!AppPrefs.isEncryptionSchemeSupported(group.schemeVersion)) {
                        throw UnsupportedGroupEncryptionSchemeException(
                            groupId = group.id,
                            schemeVersion = group.schemeVersion
                        )
                    }
                }
            }

            val membersAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionGroupMembersID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parseGroupMember() }
            }

            val settingsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionGroupSettingsID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parseGroupSettings() }
                    .takeIf { it.size == 1 && it.first().ownerUserId == appPrefs.userId }
                    ?.firstOrNull()
            }

            val locationsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parseMemberLocation() }
            }

            val placesAsync = async {
                placesRemote.getPlacesForGroup(groupId = groupId)
            }

            val group = groupAsync.await()
            val members = membersAsync.await()
            val settings = settingsAsync.await()
            val locations = locationsAsync.await()
            val places = placesAsync.await()

            return@blocking GroupWithMembersAndPlacesRaw(
                group = GroupWithMembersRaw(
                    group = group,
                    members = members.map { member ->
                        MemberWithLocationsRaw(
                            member = member.copy(settingsRaw = settings?.takeIf { it.id == member.id }),
                            locations = locations.filter { location ->
                                member.locationIds.contains(location.id)
                            }
                        )
                    }
                ),
                places = places
            )
        }
    }

    suspend fun deleteGroup(groupId: String) {
        blocking {
            val response = functionsApi.runAndWait(
                function = ServerFunction.GROUP_DELETE,
                data = groupId
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    suspend fun blockGroup(group: UserGroup) {
        return blocking {

            if (appPrefs.userId == group.ownerUserId)
                throw PermissionDeniedException(code = 403, message = "Owner can not block the group")

            val userKey = cryptoUserKeyRepo.activePersonalKeyWithVersion()
            val groupData = group.data.data?.let {
                cryptoCore.encryptModel(
                    data = BlockListModelData(
                        name = it.name,
                        image = it.image
                    ),
                    key = userKey.key
                )
            } ?: ""

            val payload = BlockGroupFunctionData(
                groupId = group.id,
                groupData = groupData,
                userKeyVersion = userKey.version
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.GROUP_BLOCK,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    suspend fun blockUser(user: MemberModel) {
        return blocking {

            if (appPrefs.userId == user.userId)
                throw PermissionDeniedException(code = 403, message = "You can not block yourself")

            val userKey = cryptoUserKeyRepo.activePersonalKeyWithVersion()
            val userData = user.memberData.data?.let {
                cryptoCore.encryptModel(
                    data = BlockListModelData(
                        name = it.name,
                        image = it.image ?: ImageModel.emoji(Emojis.robot)
                    ),
                    key = userKey.key
                )
            } ?: ""

            val payload = BlockGroupMemberFunctionData(
                groupId = user.groupId,
                userId = user.userId,
                userData = userData,
                userKeyVersion = userKey.version
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.MEMBER_BLOCK,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    suspend fun notifyLowBattery(
        groupId: String,
        memberId: String,
        locationId: String
    ) {
        blocking {
            functionsApi.runAsync(
                function = ServerFunction.EVENT_BATTERY,
                data = BatteryAlertPayload(
                    groupId = groupId,
                    memberId = memberId,
                    locationId = locationId
                )
            )
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