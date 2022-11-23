package app.paralino.android.data.repos.location

import androidx.room.*
import app.paralino.android.data.models.location.MemberEntity
import app.paralino.android.data.models.location.MemberEntityWithLocations
import app.paralino.android.data.models.location.MemberLocationEntity
import app.paralino.android.data.models.location.MemberMutableDataUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Transaction
    suspend fun recreateForGroup(groupId: String, data: List<MemberEntityWithLocations>) {
        clearAllMembersAndLocationsForGroup(groupId)
        createOrUpdateMemberOnly(*data.map { it.member }.toTypedArray())
        createOrUpdateMemberLocations(*data.flatMap { it.locations }.toTypedArray())
    }

    @Transaction
    suspend fun recreateAll(data: List<MemberEntityWithLocations>) {
        clearAll()
        createOrUpdateMemberOnly(*data.map { it.member }.toTypedArray())
        createOrUpdateMemberLocations(*data.flatMap { it.locations }.toTypedArray())
    }


    @Transaction
    suspend fun createOrUpdateMembersWithLocations(data: List<MemberEntityWithLocations>) {
        createOrUpdateMemberOnly(*data.map { it.member }.toTypedArray())
        createOrUpdateMemberLocations(*data.flatMap { it.locations }.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdateMemberOnly(vararg data: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdateMemberLocations(vararg data: MemberLocationEntity)



    @Query("SELECT * FROM members WHERE id = :memberId")
    fun getMember(memberId: String): Flow<MemberEntityWithLocations?>

    @Query("SELECT * FROM members WHERE userId = :userId")
    fun getLocationsForUser(userId: String): Flow<List<MemberEntityWithLocations>>

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    fun getLocationsForGroup(groupId: String): Flow<List<MemberEntityWithLocations>>


    @Query("SELECT COUNT(:groupId) FROM members WHERE groupId = :groupId")
    fun getMemberCountInGroup(groupId: String): Flow<Int>

    @Query("SELECT COUNT(:groupId) FROM members WHERE groupId = :groupId")
    suspend fun getMemberCountInGroupOnce(groupId: String): Int



    @Query("SELECT id FROM locations WHERE userId = :userId AND groupId = :groupId")
    suspend fun getUserLocationIds(groupId: String, userId: String): List<String>

    @Query("SELECT id FROM members WHERE userId = :userId AND groupId = :groupId")
    suspend fun getUserMemberId(groupId: String, userId: String): String?



    @Query("SELECT ownerPublicKey FROM members WHERE groupId = :groupId AND userId = :userId")
    suspend fun getMemberPublicKeyOnce(groupId: String, userId: String): String?

    @Update(onConflict = OnConflictStrategy.IGNORE, entity = MemberLocationEntity::class)
    suspend fun updateMemberMutableData(data: MemberMutableDataUpdate)



    suspend fun removeMemberWithLocationsForGroup(groupId: String, memberId: String) {
        clearMemberForGroup(groupId, memberId)
        clearMemberLocationsForGroup(groupId, memberId)
    }

    @Query("DELETE FROM members WHERE groupId = :groupId AND id = :memberId")
    suspend fun clearMemberForGroup(groupId: String, memberId: String)

    @Query("DELETE FROM locations WHERE groupId = :groupId AND memberId = :memberId")
    suspend fun clearMemberLocationsForGroup(groupId: String, memberId: String)



    @Transaction
    suspend fun clearAllMembersAndLocationsForGroup(groupId: String) {
        clearAllMembersForGroup(groupId)
        clearAllLocationsForGroup(groupId)
    }

    @Query("DELETE FROM members WHERE groupId = :groupId")
    suspend fun clearAllMembersForGroup(groupId: String)

    @Query("DELETE FROM locations WHERE groupId = :groupId")
    suspend fun clearAllLocationsForGroup(groupId: String)



    @Transaction
    suspend fun clearAll() {
        clearMembers()
        clearLocations()
    }

    @Query("DELETE FROM members")
    suspend fun clearMembers()

    @Query("DELETE FROM locations")
    suspend fun clearLocations()

}