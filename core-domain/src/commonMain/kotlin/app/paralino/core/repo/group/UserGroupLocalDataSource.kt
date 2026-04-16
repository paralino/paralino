package app.paralino.core.repo.group

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.crypto.group.VerificationResult
import app.paralino.core.models.Resource
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.location.MemberModelWithLocationsAndGroup
import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.models.mappers.GroupsMapperLocal
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import app.paralino.core.models.raw.GroupWithMembersAndPlacesRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserGroupLocalDataSource constructor(
    private val database: MainDatabaseHelper,
    private val mapper: GroupsMapperLocal,
    private val memberMapper: MemberWithLocationsMapperLocal,
    private val placeMapper: GroupPlaceMapperLocal,
    private val memberLocal: LocationLocalDataSource,
    private val placeActivity: PlaceActivityDataSource,
    private val appPrefs: AppPrefs,
    private val scopeProvider: ICoroutineScopeProvider
) {

    val allGroupsState: StateFlow<Resource<List<UserGroupWithMetadata>>> =
        getAllGroupsWithMetadata()
            .map { Resource.Ready(it) }
            .conflate()
            .distinctUntilChanged { old, new ->
                old.data.sortedBy { it.id }.toTypedArray()
                    .contentDeepEquals(new.data.sortedBy { it.id }.toTypedArray())
            }
            .stateIn(
                scope = scopeProvider.defaultIOScope,
                started = SharingStarted.Eagerly,
                initialValue = Resource.NotInitialized
            )

    suspend fun recreateGroups(data: List<GroupWithMembersAndPlacesRaw>) {
        database.groupRelatedDao.recreateGroupsWithMembersAndPlaces(
            groups = data.map { mapper.remote2Local(it.group.group) },
            members = data.flatMap { it.group.members.map { memberMapper.remote2Local(it.member) } },
            locations = data.flatMap {
                it.group.members.flatMap { member ->
                    member.locations.map { memberMapper.remote2Local(member.member, it) }
                }
            },
            places = data.flatMap { it.places.map { placeMapper.remote2Local(it.place) } },
            placeSettings = data.flatMap { it.places.mapNotNull { it.settings?.let { placeMapper.remote2Local(it) } } }
        )
        placeActivity.recreatePlaces(places = data.flatMap { it.places })
    }

    suspend fun updateOrCreateGroupWithMembersAndPlaces(group: GroupWithMembersAndPlacesRaw) {
        database.groupRelatedDao.recreateGroupWithMembersAndPlaces(
            group = mapper.remote2Local(group.group.group),
            members = group.group.members.map { memberMapper.remote2Local(it.member) },
            locations = group.group.members.flatMap { member ->
                member.locations.map { memberMapper.remote2Local(member.member, it) }
            },
            places = group.places.map { placeMapper.remote2Local(it.place) },
            placeSettings = group.places.mapNotNull { it.settings?.let { placeMapper.remote2Local(it) } }
        )
        placeActivity.recreatePlacesForGroup(groupId = group.group.group.id.asGroupOrSpaceId(), places = group.places)
    }

    suspend fun updateOrCreateMember(member: MemberWithLocationsRaw) {
        database.groupRelatedDao.createOrUpdateMemberWithLocations(
            memberEntity = memberMapper.remote2Local(member.member),
            memberLocations = member.locations.map { memberMapper.remote2Local(member.member, it) }
        )
    }

    suspend fun updateGroupOnly(group: GroupDocumentRaw) {
        database.groupRelatedDao.createGroup(mapper.remote2Local(group))
    }

//    suspend fun recreatePlaces(places: List<GroupPlaceWithSettingsRaw>) {
//        database.groupRelatedDao.recreateAllPlacesWithSettings(
//            placeEntity = places.map { placeMapper.remote2Local(it.place) },
//            placeSettings = places.mapNotNull { it.settings?.let { placeMapper.remote2Local(it) } }
//        )
//    }

    suspend fun updateOrCreatePlace(place: GroupPlaceWithSettingsRaw) {
        database.groupRelatedDao.recreatePlaceWithSettings(
            placeEntity = placeMapper.remote2Local(place.place),
            placeSettings = place.settings?.let { placeMapper.remote2Local(it) }
        )
    }

    suspend fun recreatePlacesForGroup(
        groupId: String,
        places: List<GroupPlaceWithSettingsRaw>
    ) {
        database.groupRelatedDao.recreateAllPlacesWithSettings(
            groupId = groupId,
            placeEntity = places.map { placeMapper.remote2Local(it.place) },
            placeSettings = places.mapNotNull { it.settings?.let { placeMapper.remote2Local(it) } }
        )
        placeActivity.recreatePlacesForGroup(groupId = groupId.asGroupOrSpaceId(), places = places)
    }

    // TODO if group is deleted, changes are not propagated because of filterNotNull()

    fun getGroupRawPresentation(groupId: String): Flow<Pair<GroupDocumentRaw, UserGroup>> {
        return database.groupRelatedDao.getGroup(groupId = groupId)
            .filterNotNull()
            .map { mapper.local2Remote(it) to mapper.local2Core(it) }
    }

    fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return database.groupRelatedDao.getGroup(groupId = groupId)
            .filterNotNull()
            .map { mapper.local2Core(it) }
    }

    fun getGroupWithMetadata(groupId: String): Flow<UserGroupWithMetadata> {
        return combine(
            database.groupRelatedDao.getGroup(groupId = groupId)
                .filterNotNull(),
            database.groupRelatedDao.getMember(groupId = groupId, userId = appPrefs.userId)
                .filterNotNull(),
            memberLocal.getMemberCountInGroup(groupId)
                .distinctUntilChanged(),
            database.groupRelatedDao.getLocationCountForUsersInGroup(groupId = groupId)
                .filterNotNull()
                .distinctUntilChanged(),
            database.groupRelatedDao.getPlacesCountForGroup(groupId)
                .filterNotNull()
                .distinctUntilChanged()
        ) { groupEntity, currentUserMember, memberCount, devicesPerMember, placesCount ->
            val group = mapper.local2Core(groupEntity)
            UserGroupWithMetadata(
                group = group,
                memberCount = memberCount,
                placesCount = placesCount,
                devicesPerMember = devicesPerMember,
                verification = memberMapper.local2Core(
                    member = currentUserMember,
                    memberLocations = emptyList(),
                    group = group
                ).member.verificationResult
            )
        }
    }

    fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers?> {
        return combine(
            database.groupRelatedDao.getGroup(groupId = groupId).distinctUntilChanged(),
            database.groupRelatedDao.getAllMembersForGroup(groupId = groupId).distinctFlowOfList(),
            database.groupRelatedDao.getMemberLocationsForGroup(groupId = groupId).distinctFlowOfList()
        ) { groupEntity, members, locations ->
            if(groupEntity != null && members.isNotEmpty()) {
                // TODO double check this - sometimes it crashes on logout or expired session with NoSuchElement exception
                // mostly likely because clearing database currently is not one transaction

                val group = mapper.local2Core(groupEntity)
                val mappedMembers = members.map { member ->
                    val memberLocations = locations.filter { it.memberId == member.id }
                    memberMapper.local2Core(member, memberLocations, group)
                }

                UserGroupWithMembers(
                    group = mapper.local2Core(groupEntity),
                    members = mappedMembers
                )
            } else null
        }
    }

    suspend fun getGroupWithMembersOnce(groupId: String): UserGroupWithMembers? {
        val groupEntity = database.groupRelatedDao.getGroupOnce(groupId = groupId)
        val members = database.groupRelatedDao.getAllMembersForGroupOnce(groupId = groupId)
        val locations = database.groupRelatedDao.getMemberLocationsForGroupOnce(groupId = groupId)

        if (groupEntity != null && members.isNotEmpty()) {
            val group = mapper.local2Core(groupEntity)
            val mappedMembers = members.map { member ->
                val memberLocations = locations.filter { it.memberId == member.id }
                memberMapper.local2Core(member, memberLocations, group)
            }

            return UserGroupWithMembers(
                group = mapper.local2Core(groupEntity),
                members = mappedMembers
            )
        } else {
            return null
        }
    }

    fun getAllGroupsWithMetadata(): Flow<List<UserGroupWithMetadata>> {
        return database.groupRelatedDao.getAllGroups()
            .flatMapLatest { groups ->
                if(groups.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val flows = groups.map {
                        val group = mapper.local2Core(it)
                        combine(
                            database.groupRelatedDao.getMember(groupId = group.id, userId = appPrefs.userId)
                                .filterNotNull(),
                            memberLocal.getMemberCountInGroup(group.id)
                                .distinctUntilChanged(),
                            database.groupRelatedDao.getLocationCountForUsersInGroup(groupId = group.id)
                                .filterNotNull()
                                .distinctUntilChanged(),
                            database.groupRelatedDao.getPlacesCountForGroup(group.id)
                                .filterNotNull()
                                .distinctUntilChanged()
                        ) { currentUserMember, memberCount, devicesPerMember, placesCount ->
                            UserGroupWithMetadata(
                                group = group,
                                memberCount = memberCount,
                                placesCount = placesCount,
                                devicesPerMember = devicesPerMember,
                                verification = memberMapper.local2Core(
                                    member = currentUserMember,
                                    memberLocations = emptyList(),
                                    group = group
                                ).member.verificationResult
                            )
                        }
                    }
                    combine(flows) {
                        it.toList()
                    }
                }
            }
            .distinctFlowOfList()
            .map { it.sortedBy { it.name } }
    }

    fun getGroupsCount(): Flow<Int?> {
        return database.groupRelatedDao.getGroupsCount()
    }

    fun getMemberRawPresentation(group: UserGroup?, memberId: String): Flow<Pair<MemberWithLocationsRaw, MemberModelWithLocations>> {
        return combine(
            database.groupRelatedDao.getMemberById(memberId = memberId).filterNotNull(),
            database.groupRelatedDao.getMemberLocations(memberId = memberId)
        ) { member, locations ->
            val group = group ?: getGroupOnce(member.groupId)!!
            val raw = memberMapper.local2Remote(member, locations)
            val core = memberMapper.local2Core(member, locations, group)
            raw to core
        }
    }

    fun getMemberWithLocationsAndGroup(groupId: String, memberUserId: String): Flow<MemberModelWithLocationsAndGroup?> {
        return combine(
            getGroupWithoutMembers(groupId = groupId),
            database.groupRelatedDao.getMember(groupId = groupId, userId = memberUserId).filterNotNull(),
            database.groupRelatedDao.getMemberLocations(groupId = groupId, userId = memberUserId)
        ) { group, member, locations ->
            MemberModelWithLocationsAndGroup(
                group = group,
                member = memberMapper.local2Core(member = member, memberLocations = locations, group = group)
            )
        }
    }

    suspend fun getMemberModelsForUser(userId: String): List<MemberModel> {
        return database.groupRelatedDao.getAllMembersForUserOnce(userId = userId).map {
            val group = getGroupOnce(it.groupId)!!
            memberMapper.local2Core(it, emptyList(), group).member
        }
    }

    suspend fun getMemberOnce(groupId: String, userId: String): MemberModel? {
        return database.groupRelatedDao.getMemberOnce(
            groupId = groupId,
            userId = userId
        )?.let {
            val group = getGroupOnce(it.groupId)!!
            memberMapper.local2Core(it, emptyList(), group).member
        }
    }

    suspend fun getMemberNameOnce(groupId: String, userId: String): String? {
        return database.groupRelatedDao.getMemberOnce(
            groupId = groupId,
            userId = userId
        )?.let {
            memberMapper.coreMapper.getMemberData(
                model = memberMapper.local2Remote(it, emptyList()).member,
                // Not doing verification here since we just need a name, regardless of the status
                verification = VerificationResult.Verified,
                groupOwnerUserId = getGroupOwnerId(groupId = groupId)
            ).data?.name
        }
    }

    suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupRelatedDao.getGroupOwnerUserId(groupId)
    }

    suspend fun getGroupOnce(groupId: String): UserGroup? {
        return database.groupRelatedDao.getGroupOnce(groupId = groupId)
            ?.let { mapper.local2Core(it) }
    }

    suspend fun deleteGroup(groupId: String) {
        database.groupRelatedDao.deleteAllGroupDataForGroup(groupId = groupId)
    }

    suspend fun removeMember(groupId: String, userId: String) {
        database.groupRelatedDao.removeMemberDataFromGroup(groupId = groupId, userId = userId)
    }

    suspend fun deletePlace(placeId: String) {
        database.groupRelatedDao.deletePlaceData(placeId = placeId)
    }
}