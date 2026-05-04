package app.paralino.core.repo.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.BatteryNotificationsSettingsData
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.BlockGroupFunctionData
import app.paralino.core.models.raw.BlockGroupMemberFunctionData
import app.paralino.core.models.raw.DeleteGroupMemberPayload
import app.paralino.core.models.raw.GroupBatteryNotificationsUpdatePayload
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.GroupWithMembersAndPlacesRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.models.raw.UpdateGroupPayload
import app.paralino.core.models.raw.UserDeviceLocationRaw
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.models.raw.UserDeviceWithLocationRaw
import app.paralino.core.models.raw.UserDevicesWithLocationsAndPlacesRaw
import app.paralino.core.models.user.BlockListModelData
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.places.PlacesRemoteSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.PermissionDeniedException
import app.paralino.core.utils.exceptions.UnsupportedGroupEncryptionSchemeException
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class GroupsRemoteSource constructor(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val placesRemote: PlacesRemoteSource,
    private val appConfig: AppConfigRepository,
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

            val payload = GroupBatteryNotificationsUpdatePayload(
                groupId = groupId,
                memberId = memberId,
                alertData = BaseRemoteModule.formatter.encodeToString(batterySettings)
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.ALERTS_BATTERY_UPDATE_GROUP,
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
                throw PermissionDeniedException(
                    code = 403,
                    message = "Owner can not be removed from the group"
                )

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

    suspend fun getAllGroupsWithMembers(): List<GroupWithMembersRaw> {
        return blocking {
            val groups = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = appConfig.resourceIds.collectionGroupsID
            )
                .map { it.parseGroup() }
                .filter {
                    AppPrefs.isEncryptionSchemeSupported(
                        schemeVersion = it.schemeVersion,
                        supportedEncryptionScheme = AppPrefs.supportedGroupEncryptionScheme
                    )
                }

            if (groups.isEmpty()) {
                // no need to fetch other resources if there are no groups
                return@blocking emptyList()
            }

            val membersAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionGroupMembersID
                ).map { it.parseGroupMember() }
            }

            val settingsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionGroupSettingsID
                ).map { it.parseGroupSettings() }
            }

            val locationsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionMemberLocationsID,
                ).map { it.parseMemberLocation() }
            }

            val members = membersAsync.await()
            val settings = settingsAsync.await()
            val locations = locationsAsync.await()

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

    suspend fun getAllLocationResources(): GroupsMembersLocationsPlacesRaw {
        return blocking {
            val groups = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = appConfig.resourceIds.collectionGroupsID
            )
                .map { it.parseGroup() }
                .filter {
                    AppPrefs.isEncryptionSchemeSupported(
                        schemeVersion = it.schemeVersion,
                        supportedEncryptionScheme = AppPrefs.supportedGroupEncryptionScheme
                    )
                }

            val places = placesRemote.getAllPlaces()

            val groupsData = if (groups.isEmpty()) {
                // no need to fetch other resources if there are no groups
                emptyList()
            } else {
                val membersAsync = async {
                    api.listAllDocuments(
                        databaseType = BackendDatabaseType.DATA,
                        collectionId = appConfig.resourceIds.collectionGroupMembersID
                    ).map { it.parseGroupMember() }
                }

                val settingsAsync = async {
                    api.listAllDocuments(
                        databaseType = BackendDatabaseType.DATA,
                        collectionId = appConfig.resourceIds.collectionGroupSettingsID
                    ).map { it.parseGroupSettings() }
                }

                val locationsAsync = async {
                    api.listAllDocuments(
                        databaseType = BackendDatabaseType.DATA,
                        collectionId = appConfig.resourceIds.collectionMemberLocationsID,
                    ).map { it.parseMemberLocation() }
                }

                val members = membersAsync.await()
                val settings = settingsAsync.await()
                val locations = locationsAsync.await()

                groups.map { group ->
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
                        places = places.filter {
                            it.place.isInPersonalSpace != true && it.place.groupId == group.id
                        }
                    )
                }
            }

            val personalDevicesAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionUserDevicesID,
                ).map { it.parsePersonalDevice() }
            }

            val personalLocationsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionPersonalLocationsID,
                ).map { it.parsePersonalDeviceLocation() }
            }

            val personalDevices = personalDevicesAsync.await()
            val personalLocations = personalLocationsAsync.await()

            GroupsMembersLocationsPlacesRaw(
                groups = groupsData,
                personal = UserDevicesWithLocationsAndPlacesRaw(
                    devices = personalDevices.map { device ->
                        UserDeviceWithLocationRaw(
                            device = device,
                            location = personalLocations.firstOrNull { it.id == device.id }
                        )
                    },
                    places = places.filter {
                        it.place.isInPersonalSpace == true
                    }
                )
            )
        }
    }

    suspend fun getGroupWithMembers(groupId: String): GroupWithMembersAndPlacesRaw {
        return blocking {
            val group = try {
                api.getDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionGroupsID,
                    documentId = groupId
                ).parseGroup()
            } catch (e: Exception) {
                if (e is NotFoundException) {
                    // try once again
                    delay(200)
                    api.getDocument(
                        databaseType = BackendDatabaseType.DATA,
                        collectionId = appConfig.resourceIds.collectionGroupsID,
                        documentId = groupId
                    ).parseGroup()
                } else {
                    throw e
                }
            }

            val isEncryptionSchemeSupported = AppPrefs.isEncryptionSchemeSupported(
                schemeVersion = group.schemeVersion,
                supportedEncryptionScheme = AppPrefs.supportedGroupEncryptionScheme
            )

            if (!isEncryptionSchemeSupported) {
                throw UnsupportedGroupEncryptionSchemeException(
                    groupId = group.id,
                    schemeVersion = group.schemeVersion
                )
            }

            val membersAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionGroupMembersID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parseGroupMember() }
            }

            val settingsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionGroupSettingsID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parseGroupSettings() }
                    .takeIf { it.size == 1 && it.first().ownerUserId == appPrefs.userId }
                    ?.firstOrNull()
            }

            val locationsAsync = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionMemberLocationsID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parseMemberLocation() }
            }

            val placesAsync = async {
                placesRemote.getPlacesForGroup(groupId = groupId)
            }

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
                throw PermissionDeniedException(
                    code = 403,
                    message = "Owner can not block the group"
                )

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

    private fun Map<String, Any>.parsePersonalDeviceLocation(): UserDeviceLocationRaw {
        return UserDeviceLocationRaw.from(this)
    }

    private fun Map<String, Any>.parsePersonalDevice(): UserDeviceRaw {
        return UserDeviceRaw.from(this)
    }
}

data class GroupsMembersLocationsPlacesRaw(
    val groups: List<GroupWithMembersAndPlacesRaw>,
    val personal: UserDevicesWithLocationsAndPlacesRaw
)