package app.paralino.core.db.main.groups

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.paralino.core.db.main.places.PendingPlaceEventsEntity
import app.paralino.core.db.main.places.PlaceEntity
import app.paralino.core.db.main.places.PlaceSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupRelatedEntitiesDao {

    // Group Entity operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroups(groups: List<GroupEntity>)

    @Query("SELECT * FROM groupEntity WHERE id = :groupId")
    fun getGroup(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groupEntity WHERE id = :groupId")
    suspend fun getGroupOnce(groupId: String): GroupEntity?

    @Query("SELECT * FROM groupEntity")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT COUNT(id) FROM groupEntity")
    fun getGroupsCount(): Flow<Int?>

    @Query("SELECT ownerUserId FROM groupEntity WHERE id = :groupId")
    suspend fun getGroupOwnerUserId(groupId: String): String?

    @Query("DELETE FROM groupEntity WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM groupEntity")
    suspend fun clearAllGroups()



    // Member Entity operations
    @Transaction
    suspend fun createOrUpdateMembersWithoutSettings(members: List<MemberEntity>) {
        val mappedMembers = members.map { member ->
            val currentMember = getMemberByIdOnce(memberId = member.id)
            member.copy(
                settingsId = currentMember?.settingsId,
                settingsGroupId = currentMember?.settingsGroupId,
                settingsOwnerUserId = currentMember?.settingsOwnerUserId,
                settingsMemberIntegrity = currentMember?.settingsMemberIntegrity,
                settingsGroupIntegrity = currentMember?.settingsGroupIntegrity,
                settingsMemberIntegrityUserKeyVersion = currentMember?.settingsMemberIntegrityUserKeyVersion,
                settingsGroupIntegrityUserKeyVersion = currentMember?.settingsGroupIntegrityUserKeyVersion
            )
        }

        createMembers(mappedMembers)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createMembers(members: List<MemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createMember(member: MemberEntity)

    @Query("SELECT * FROM memberEntity WHERE groupId = :groupId")
    fun getAllMembersForGroup(groupId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM memberEntity WHERE groupId = :groupId")
    suspend fun getAllMembersForGroupOnce(groupId: String): List<MemberEntity>

    @Query("SELECT * FROM memberEntity WHERE userId = :userId")
    suspend fun getAllMembersForUserOnce(userId: String): List<MemberEntity>

    @Query("SELECT * FROM memberEntity WHERE id = :memberId")
    suspend fun getMemberByIdOnce(memberId: String): MemberEntity?

    @Query("SELECT * FROM memberEntity WHERE id = :memberId")
    fun getMemberById(memberId: String): Flow<MemberEntity?>

    @Query("SELECT * FROM memberEntity WHERE groupId = :groupId AND userId = :userId")
    fun getMember(groupId: String, userId: String): Flow<MemberEntity?>

    @Query("SELECT * FROM memberEntity WHERE groupId = :groupId AND userId = :userId")
    suspend fun getMemberOnce(groupId: String, userId: String): MemberEntity?

    @Query("SELECT id FROM memberEntity WHERE memberEntity.groupId = :groupId AND memberEntity.userId = :userId")
    suspend fun getUserMemberId(groupId: String, userId: String): String?

    @Query("SELECT COUNT(id) FROM memberEntity WHERE groupId = :groupId")
    fun getMemberCount(groupId: String): Flow<Int?>

    @Query("DELETE FROM memberEntity WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteMember(groupId: String, userId: String)

    @Query("DELETE FROM memberEntity WHERE groupId = :groupId")
    suspend fun clearMembersForGroup(groupId: String)

    @Query("DELETE FROM memberEntity")
    suspend fun clearAllMembers()



    // Place Entity operations
    @Transaction
    suspend fun recreatePlaceWithSettings(
        placeEntity: PlaceEntity,
        placeSettings: PlaceSettingsEntity?
    ) {
        createPlace(placeEntity)

        deleteSettingsForPlace(placeId = placeEntity.id)
        if (placeSettings != null) {
            createPlaceSettings(placeSettings)
        }
    }

    @Transaction
    suspend fun recreateAllPlacesWithSettings(
        placeEntity: List<PlaceEntity>,
        placeSettings: List<PlaceSettingsEntity>
    ) {
        clearAllPlaces()
        clearAllPlaceSettings()

        createPlaces(placeEntity)
        createPlaceSettings(placeSettings)
    }

    @Transaction
    suspend fun recreateAllPlacesWithSettings(
        groupId: String,
        placeEntity: List<PlaceEntity>,
        placeSettings: List<PlaceSettingsEntity>
    ) {
        deletePlacesForGroup(groupId = groupId)
        deleteSettingsForGroup(groupId = groupId)

        createPlaces(placeEntity)
        createPlaceSettings(placeSettings)
    }

    @Transaction
    suspend fun deletePlaceData(placeId: String) {
        deletePlace(placeId = placeId)
        deleteSettingsForPlace(placeId = placeId)
        deletePendingEventsForPlace(placeId = placeId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlace(place: PlaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaces(places: List<PlaceEntity>)

    @Query("SELECT * FROM placeEntity WHERE groupId = :groupId")
    fun getAllPlacesForGroup(groupId: String): Flow<List<PlaceEntity>>

    @Query("SELECT COUNT(id) FROM placeEntity WHERE groupId = :groupId")
    suspend fun getPlacesCountForGroupOnce(groupId: String): Int?

    @Query("SELECT * FROM placeEntity")
    suspend fun getAllPlacesOnce(): List<PlaceEntity>

    @Query("SELECT * FROM placeEntity")
    fun getAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM placeEntity WHERE id = :placeId")
    suspend fun getPlaceOnce(placeId: String): PlaceEntity?

    @Query("SELECT * FROM placeEntity WHERE id = :placeId")
    fun getPlace(placeId: String): Flow<PlaceEntity?>

    @Query("SELECT groupId FROM placeEntity WHERE id = :placeId")
    suspend fun getGroupIdForPlace(placeId: String): String?

    @Query("DELETE FROM placeEntity WHERE id = :placeId")
    suspend fun deletePlace(placeId: String)

    @Query("DELETE FROM placeEntity WHERE groupId = :groupId")
    suspend fun deletePlacesForGroup(groupId: String)

    @Query("DELETE FROM placeEntity")
    suspend fun clearAllPlaces()



    // Place Settings Entity operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaceSettings(settings: PlaceSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaceSettings(settings: List<PlaceSettingsEntity>)

    @Query("SELECT * FROM placeSettingsEntity WHERE placeId = :placeId")
    fun getSettingsForPlace(placeId: String): Flow<PlaceSettingsEntity?>

    @Query("DELETE FROM placeSettingsEntity WHERE placeId = :placeId")
    suspend fun deleteSettingsForPlace(placeId: String)

    @Query("DELETE FROM placeSettingsEntity WHERE groupId = :groupId")
    suspend fun deleteSettingsForGroup(groupId: String)

    @Query("DELETE FROM placeSettingsEntity")
    suspend fun clearAllPlaceSettings()



    // Member Location Entity operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createMemberLocations(locations: List<MemberLocationEntity>)

    @Query("SELECT * FROM memberLocationEntity WHERE memberId = :memberId")
    fun getMemberLocations(memberId: String): Flow<List<MemberLocationEntity>>

    @Query("SELECT * FROM memberLocationEntity WHERE groupId = :groupId")
    fun getMemberLocationsForGroup(groupId: String): Flow<List<MemberLocationEntity>>

    @Query("SELECT * FROM memberLocationEntity WHERE memberId = :memberId")
    suspend fun getMemberLocationsOnce(memberId: String): List<MemberLocationEntity>

    @Query("SELECT * FROM memberLocationEntity WHERE userId = :userId")
    suspend fun getMemberLocationsForUserOnce(userId: String): List<MemberLocationEntity>

    @Update(entity = MemberLocationEntity::class)
    suspend fun updateMemberLocation(dataUpdate: MemberLocationEntityDataUpdate)

    @Query("DELETE FROM memberLocationEntity WHERE groupId = :groupId")
    suspend fun clearLocationsForGroup(groupId: String)

    @Query("DELETE FROM memberLocationEntity WHERE groupId = :groupId AND userId = :userId")
    suspend fun clearLocationsForUser(groupId: String, userId: String)

    @Query("DELETE FROM memberLocationEntity WHERE memberId = :memberId")
    suspend fun clearLocationsForMember(memberId: String)

    @Query("SELECT COUNT(id) FROM memberLocationEntity WHERE groupId = :groupId AND userId = :userId")
    fun getLocationCountForUserInGroup(groupId: String, userId: String): Flow<Int?>

    @Query("UPDATE OR IGNORE memberLocationEntity SET locationData = NULL WHERE id = :locationId")
    suspend fun clearLocationDataForLocation(locationId: String)

    @Query("DELETE FROM memberLocationEntity")
    suspend fun clearAllMemberLocations()



    // Pending Place Events Entity operations
    @Query("SELECT * FROM pendingPlaceEventsEntity")
    suspend fun getAllPendingEventsOnce(): List<PendingPlaceEventsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPendingEvent(event: PendingPlaceEventsEntity)

    @Query("DELETE FROM pendingPlaceEventsEntity WHERE id = :id")
    suspend fun deletePendingEvent(id: Long)

    @Query("DELETE FROM pendingPlaceEventsEntity WHERE groupId = :groupId")
    suspend fun deletePendingEventsForGroup(groupId: String)

    @Query("DELETE FROM pendingPlaceEventsEntity WHERE placeId = :placeId")
    suspend fun deletePendingEventsForPlace(placeId: String)

    @Query("DELETE FROM pendingPlaceEventsEntity WHERE notifyTimeMillis < :maxPendingTimeMillis")
    suspend fun deleteObsoleteEvents(maxPendingTimeMillis: Long)

    @Query("DELETE FROM pendingPlaceEventsEntity")
    suspend fun clearAllPendingEvents()



    // Transaction operations

    @Transaction
    suspend fun recreateGroupsWithMembersAndPlaces(
        groups: List<GroupEntity>,
        members: List<MemberEntity>,
        locations: List<MemberLocationEntity>,
        places: List<PlaceEntity>,
        placeSettings: List<PlaceSettingsEntity>
    ) {
        clearAllGroups()
        clearAllMembers()
        clearAllMemberLocations()
        clearAllPlaces()
        clearAllPlaceSettings()

        createGroups(groups)
        createMembers(members)
        createMemberLocations(locations)
        createPlaces(places)
        createPlaceSettings(placeSettings)
    }

    @Transaction
    suspend fun recreateGroupWithMembersAndPlaces(
        group: GroupEntity,
        members: List<MemberEntity>,
        locations: List<MemberLocationEntity>,
        places: List<PlaceEntity>,
        placeSettings: List<PlaceSettingsEntity>
    ) {
        clearMembersForGroup(groupId = group.id)
        clearLocationsForGroup(groupId = group.id)
        deletePlacesForGroup(groupId = group.id)
        deleteSettingsForGroup(groupId = group.id)

        createGroup(group)
        createMembers(members)
        createMemberLocations(locations)
        createPlaces(places)
        createPlaceSettings(placeSettings)
    }

    @Transaction
    suspend fun clearAllEntities() {
        clearAllGroups()
        clearAllMembers()
        clearAllPlaces()
        clearAllPlaceSettings()
        clearAllMemberLocations()
        clearAllPendingEvents()
    }

    @Transaction
    suspend fun deleteAllGroupDataForGroup(groupId: String) {
        deleteGroup(groupId = groupId)
        clearMembersForGroup(groupId = groupId)
        deletePlacesForGroup(groupId = groupId)
        deleteSettingsForGroup(groupId = groupId)
        clearLocationsForGroup(groupId = groupId)
        deletePendingEventsForGroup(groupId = groupId)
    }

    @Transaction
    suspend fun updatePendingEventsData(
        sentEvents: List<Long>,
        maxPendingTimeMillis: Long
    ) {
        sentEvents.forEach {
            try {
                deletePendingEvent(it)
            } catch (e: Exception) {
                // ignore if any
            }
        }
        deleteObsoleteEvents(maxPendingTimeMillis)
    }

    @Transaction
    suspend fun createOrUpdateMemberWithLocations(
        memberEntity: MemberEntity,
        memberLocations: List<MemberLocationEntity>
    ) {
        createMember(memberEntity)
        clearLocationsForMember(memberId = memberEntity.id)
        createMemberLocations(memberLocations)
    }

    @Transaction
    suspend fun removeMemberDataFromGroup(groupId: String, userId: String) {
        deleteMember(groupId = groupId, userId = userId)
        clearLocationsForUser(groupId = groupId, userId = userId)
    }
}
