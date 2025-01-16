package app.paralino.core.repo.group

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.domain.crypto.VerificationResultError
import app.paralino.core.models.Resource
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithLimits
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
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
        placeActivity.recreatePlacesForGroup(groupId = group.group.group.id, places = group.places)
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
        placeActivity.recreatePlacesForGroup(groupId = groupId, places = places)
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
            database.groupRelatedDao.getGroup(groupId = groupId).filterNotNull(),
            memberLocal.getMemberCountInGroup(groupId),
            database.groupRelatedDao.getMember(groupId = groupId, userId = appPrefs.userId).filterNotNull()
        ) { groupEntity, memberCount, currentUserMember ->
            val group = mapper.local2Core(groupEntity)
            UserGroupWithMetadata(
                group = group,
                memberCount = memberCount,
                verification = memberMapper.local2Core(
                    member = currentUserMember,
                    memberLocations = emptyList(),
                    group = group
                ).member.verificationResult
            )
        }
    }

    fun getGroupWithLimits(groupId: String): Flow<UserGroupWithLimits> {
        return combine(
            database.groupRelatedDao.getGroup(groupId = groupId)
                .filterNotNull(),
            memberLocal.getMemberCountInGroup(groupId),
            database.groupRelatedDao.getLocationCountForUserInGroup(groupId = groupId, userId = appPrefs.userId)
                .filterNotNull()
                .distinctUntilChanged(),
            database.groupRelatedDao.getPlacesCountForGroup(groupId)
                .filterNotNull()
                .distinctUntilChanged()
        ) { groupEntity, memberCount, devicesCount, placesCount ->
            val group = mapper.local2Core(groupEntity)
            UserGroupWithLimits(
                group = group,
                memberCount = memberCount,
                placesCount = placesCount,
                devicesCount = devicesCount
            )
        }
    }

    suspend fun getGroupWithLimitsOnce(groupId: String): UserGroupWithLimits? {
        return getGroupWithLimits(groupId = groupId).firstOrNull()
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

    fun getAllGroupsWithMetadata(): Flow<List<UserGroupWithMetadata>> {
        return database.groupRelatedDao.getAllGroups()
            .flatMapLatest { groups ->
                if(groups.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val flows = groups.map { group ->
                        memberLocal.getMemberCountInGroup(group.id)
                            .map { group to it }
                    }
                    combine(flows) {
                        it.map {
                            val group = mapper.local2Core(it.first)
                            val verificationResult = memberLocal.getMemberOnce(group, appPrefs.userId)?.verificationResult
                                ?: VerificationResult.Unverified(errors = listOf(VerificationResultError.MEMBER_VERIFICATION_MISSING))
                            UserGroupWithMetadata(
                                group = group,
                                memberCount = it.second,
                                verification = verificationResult
                            )
                        }
                    }
                }
            }
            .distinctFlowOfList()
            .map { it.sortedBy { it.name } }
    }

    fun getGroupsCount(): Flow<Int?> {
        return database.groupRelatedDao.getGroupsCount()
    }

    suspend fun getMemberModelsWithLocationForUser(userId: String): List<MemberModelWithLocations> {
        val members = database.groupRelatedDao.getAllMembersForUserOnce(userId = userId)
        val locations = database.groupRelatedDao.getMemberLocationsForUserOnce(userId = userId)

        return members.mapNotNull { member ->
            val group = getGroupOnce(member.groupId)
                ?: return@mapNotNull null

            val memberLocations = locations.filter {
                it.memberId == member.id
            }
            memberMapper.local2Core(
                member = member,
                memberLocations = memberLocations,
                group = group
            )
        }
    }

    suspend fun getMemberWithLocationsOnce(
        groupId: String,
        userId: String
    ): MemberModelWithLocations? {
        val member = database.groupRelatedDao.getMemberOnce(
            groupId = groupId,
            userId = userId
        ) ?: return null
        val locations = database.groupRelatedDao.getMemberLocationsOnce(memberId = member.id)
        val group = getGroupOnce(groupId) ?: return null

        return memberMapper.local2Core(
            member = member,
            memberLocations = locations,
            group = group
        )
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

    suspend fun clearAll() {
        database.groupRelatedDao.clearAllEntities()
    }
}