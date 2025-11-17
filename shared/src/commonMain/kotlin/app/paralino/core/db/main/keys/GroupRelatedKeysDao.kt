package app.paralino.core.db.main.keys

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GroupRelatedKeysDao {

    @Transaction
    suspend fun recreateAllKeys(
        groupKeys: List<GroupKeysEntity>,
        memberKeys: List<GroupMembersKeysEntity>,
        deviceKeys: List<GroupDeviceKeysEntity>,
        personalKeys: List<PersonalGroupKeyEntity>
    ) {
        clearAllGroupKeys()
        clearAllGroupMembersKeys()
        clearAllGroupDeviceKeys()
        clearAllActivePersonalGroupKeys()

        createGroupKeys(groupKeys)
        createGroupMemberKeys(memberKeys)
        createGroupDeviceKeys(deviceKeys)
        createPersonalGroupKeys(personalKeys)
    }

    @Transaction
    suspend fun recreateAllKeys(
        groupId: String,
        groupKeys: List<GroupKeysEntity>,
        memberKeys: List<GroupMembersKeysEntity>,
        deviceKeys: List<GroupDeviceKeysEntity>,
        personalKeys: List<PersonalGroupKeyEntity>
    ) {
        clearAllGroupKeys(groupId = groupId)
        clearAllGroupMembersKeys(groupId = groupId)
        clearAllGroupDeviceKeys(groupId = groupId)
        clearAllActivePersonalGroupKeys(groupId = groupId)

        createGroupKeys(groupKeys)
        createGroupMemberKeys(memberKeys)
        createGroupDeviceKeys(deviceKeys)
        createPersonalGroupKeys(personalKeys)
    }

    @Transaction
    suspend fun recreateGroupKeys(
        groupId: String,
        groupKeys: List<GroupKeysEntity>,
    ) {
        clearAllGroupKeys(groupId = groupId)
        createGroupKeys(groupKeys)
    }

    @Transaction
    suspend fun recreateMemberKeys(
        groupId: String,
        memberKeys: List<GroupMembersKeysEntity>,
        personalKeys: List<PersonalGroupKeyEntity>
    ) {
        clearAllGroupMembersKeys(groupId = groupId)
        clearAllActivePersonalGroupKeys(groupId = groupId)

        createGroupMemberKeys(memberKeys)
        createPersonalGroupKeys(personalKeys)
    }

    @Transaction
    suspend fun recreateDeviceKeys(
        groupId: String,
        deviceKeys: List<GroupDeviceKeysEntity>
    ) {
        clearAllGroupDeviceKeys(groupId = groupId)
        createGroupDeviceKeys(deviceKeys)
    }

    @Transaction
    suspend fun clearAllGroupRelatedKeys(groupId: String) {
        clearAllGroupKeys(groupId = groupId)
        clearAllGroupMembersKeys(groupId = groupId)
        clearAllGroupDeviceKeys(groupId = groupId)
        clearAllPersonalGroupKeys(groupId = groupId)
        clearAllGroupDeviceIntegrityKeys(groupId = groupId)
        clearAllLocalGroupDeviceSenderKeys(groupId = groupId)
    }

    @Transaction
    suspend fun clearAllKeysForUser(groupId: String, userId: String) {
        clearAllGroupMembersKeys(groupId = groupId, userId = userId)
        clearAllGroupDeviceKeys(groupId = groupId, userId = userId)
    }


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupKeys(keys: List<GroupKeysEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupMemberKeys(keys: List<GroupMembersKeysEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupDeviceKeys(keys: List<GroupDeviceKeysEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPersonalGroupKeys(keys: List<PersonalGroupKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupDeviceIntegrityKey(key: GroupDeviceIntegrityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createLocalGroupDeviceSenderKey(key: LocalGroupDeviceKeysEntity)



    @Query("SELECT * FROM groupKeysEntity")
    suspend fun getAllGroupKeys(): List<GroupKeysEntity>

    @Query("SELECT * FROM groupMembersKeysEntity")
    suspend fun getAllGroupMembersKeys(): List<GroupMembersKeysEntity>

    @Query("SELECT * FROM groupDeviceKeysEntity")
    suspend fun getAllGroupDeviceKeys(): List<GroupDeviceKeysEntity>

    @Query("SELECT * FROM personalGroupKeyEntity WHERE isArchived = 1")
    suspend fun getAllArchivedPersonalGroupKeys(): List<PersonalGroupKeyEntity>

    @Query("SELECT * FROM personalGroupKeyEntity WHERE isArchived = 0")
    suspend fun getAllActivePersonalGroupKeys(): List<PersonalGroupKeyEntity>

    @Query("SELECT * FROM groupDeviceIntegrityEntity WHERE id = :groupId")
    suspend fun getGroupDeviceIntegrityKey(groupId: String): GroupDeviceIntegrityEntity?

    @Query("SELECT * FROM localDeviceKeysEntity WHERE id = :groupId")
    suspend fun getLocalGroupDeviceSenderKey(groupId: String): LocalGroupDeviceKeysEntity?



    @Query("DELETE FROM groupKeysEntity")
    suspend fun clearAllGroupKeys()

    @Query("DELETE FROM groupMembersKeysEntity")
    suspend fun clearAllGroupMembersKeys()

    @Query("DELETE FROM groupDeviceKeysEntity")
    suspend fun clearAllGroupDeviceKeys()

    @Query("DELETE FROM personalGroupKeyEntity")
    suspend fun clearAllPersonalGroupKeys()

    @Query("DELETE FROM personalGroupKeyEntity WHERE isArchived = 0")
    suspend fun clearAllActivePersonalGroupKeys()

    @Query("DELETE FROM groupDeviceIntegrityEntity")
    suspend fun clearAllGroupDeviceIntegrityKeys()

    @Query("DELETE FROM localDeviceKeysEntity")
    suspend fun clearAllLocalGroupDeviceSenderKeys()



    @Query("DELETE FROM groupKeysEntity WHERE groupId = :groupId")
    suspend fun clearAllGroupKeys(groupId: String)

    @Query("DELETE FROM groupMembersKeysEntity WHERE groupId = :groupId")
    suspend fun clearAllGroupMembersKeys(groupId: String)

    @Query("DELETE FROM groupDeviceKeysEntity WHERE groupId = :groupId")
    suspend fun clearAllGroupDeviceKeys(groupId: String)

    @Query("DELETE FROM personalGroupKeyEntity WHERE groupId = :groupId")
    suspend fun clearAllPersonalGroupKeys(groupId: String)

    @Query("DELETE FROM personalGroupKeyEntity WHERE groupId = :groupId AND isArchived = 0")
    suspend fun clearAllActivePersonalGroupKeys(groupId: String)

    @Query("DELETE FROM groupDeviceIntegrityEntity WHERE id = :groupId")
    suspend fun clearAllGroupDeviceIntegrityKeys(groupId: String)

    @Query("DELETE FROM localDeviceKeysEntity WHERE id = :groupId")
    suspend fun clearAllLocalGroupDeviceSenderKeys(groupId: String)



    @Query("DELETE FROM groupMembersKeysEntity WHERE groupId = :groupId AND ownerUserId = :userId")
    suspend fun clearAllGroupMembersKeys(groupId: String, userId: String)

    @Query("""
        DELETE FROM groupDeviceKeysEntity 
        WHERE groupId = :groupId AND (memberUserId = :userId OR ownerUserId = :userId)
    """)
    suspend fun clearAllGroupDeviceKeys(groupId: String, userId: String)
}
