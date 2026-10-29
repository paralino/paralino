package app.paralino.core.db.main.common

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface LogoutDao {

    @Transaction
    suspend fun logout() {
        clearUser()
        clearAllPlans()
        clearAllUserDevices()
        clearAllUserConfig()
        clearAllPersonalCodes()
        clearAllUserKeys()
        clearAllBlocklist()
        clearAllSharingContracts()
        clearAllGroupsObject()
        clearAllMembers()
        clearAllPlaces()
        clearAllPlaceSettings()
        clearAllMemberLocations()
        clearAllPendingEvents()
        clearAllBatteryAlertEvents()
        deleteGroupLocationHistory()
        deletePersonalLocationHistory()
        deleteGroupLocationHistoryKeys()
        deletePersonalLocationHistoryKeys()
        clearAllPendingHistory()
        clearAllUnconfirmedHistory()
        clearAllGroupLocalHistoryKeys()
        clearAllPersonalLocalHistoryKeys()
        clearUnsyncedGroupHistoryKeys()
        clearAllGroupKeys()
        clearAllGroupMembersKeys()
        clearAllGroupDeviceKeys()
        clearAllPersonalGroupKeys()
        clearAllGroupDeviceIntegrityKeys()
        clearAllLocalGroupDeviceSenderKeys()
        clearAllPlaceActivity()
        clearAllApproximateSharedLocations()
        clearAllPrivacyZones()
        clearAllPrivacyZoneSharedLocations()
        clearAllPendingGroupJoinWelcomes()
        clearAllPendingKeyRefreshes()
        clearAllArchivedGroupDeviceKeys()
    }

    @Query("DELETE FROM userEntity")
    suspend fun clearUser()

    @Query("DELETE FROM subscriptionPlanEntity")
    suspend fun clearAllPlans()

    @Query("DELETE FROM userDeviceEntity")
    suspend fun clearAllUserDevices()

    @Query("DELETE FROM userConfigEntity")
    suspend fun clearAllUserConfig()

    @Query("DELETE FROM personalCodeEntity")
    suspend fun clearAllPersonalCodes()

    @Query("DELETE FROM userKeysEntity")
    suspend fun clearAllUserKeys()

    @Query("DELETE FROM blocklistEntity")
    suspend fun clearAllBlocklist()

    @Query("DELETE FROM sharingContractEntity")
    suspend fun clearAllSharingContracts()

    @Query("DELETE FROM groupEntity")
    suspend fun clearAllGroupsObject()

    @Query("DELETE FROM memberEntity")
    suspend fun clearAllMembers()

    @Query("DELETE FROM placeEntity")
    suspend fun clearAllPlaces()

    @Query("DELETE FROM placeSettingsEntity")
    suspend fun clearAllPlaceSettings()

    @Query("DELETE FROM memberLocationEntity")
    suspend fun clearAllMemberLocations()

    @Query("DELETE FROM pendingPlaceEventsEntity")
    suspend fun clearAllPendingEvents()

    @Query("DELETE FROM batteryAlertEvent")
    suspend fun clearAllBatteryAlertEvents()

    @Query("DELETE FROM groupHistoryEntity")
    suspend fun deleteGroupLocationHistory()

    @Query("DELETE FROM personalHistoryEntity")
    suspend fun deletePersonalLocationHistory()

    @Query("DELETE FROM groupHistoryKeysEntity")
    suspend fun deleteGroupLocationHistoryKeys()

    @Query("DELETE FROM personalHistoryKeysEntity")
    suspend fun deletePersonalLocationHistoryKeys()

    @Query("DELETE FROM pendingLocationHistoryEntity")
    suspend fun clearAllPendingHistory()

    @Query("DELETE FROM unconfirmedLocationHistoryEntity")
    suspend fun clearAllUnconfirmedHistory()

    @Query("DELETE FROM groupLocalHistoryKeyEntity")
    suspend fun clearAllGroupLocalHistoryKeys()

    @Query("DELETE FROM personalLocalHistoryKeyEntity")
    suspend fun clearAllPersonalLocalHistoryKeys()

    @Query("DELETE FROM unsyncedGroupHistoryKeyEntity")
    suspend fun clearUnsyncedGroupHistoryKeys()

    @Query("DELETE FROM groupKeysEntity")
    suspend fun clearAllGroupKeys()

    @Query("DELETE FROM groupMembersKeysEntity")
    suspend fun clearAllGroupMembersKeys()

    @Query("DELETE FROM groupDeviceKeysEntity")
    suspend fun clearAllGroupDeviceKeys()

    @Query("DELETE FROM personalGroupKeyEntity")
    suspend fun clearAllPersonalGroupKeys()

    @Query("DELETE FROM groupDeviceIntegrityEntity")
    suspend fun clearAllGroupDeviceIntegrityKeys()

    @Query("DELETE FROM localDeviceKeysEntity")
    suspend fun clearAllLocalGroupDeviceSenderKeys()

    @Query("DELETE FROM placeActivityEntity")
    suspend fun clearAllPlaceActivity()

    @Query("DELETE FROM approximateSharedLocationEntity")
    suspend fun clearAllApproximateSharedLocations()

    @Query("DELETE FROM privacyZoneEntity")
    suspend fun clearAllPrivacyZones()

    @Query("DELETE FROM privacyZoneSharedLocationEntity")
    suspend fun clearAllPrivacyZoneSharedLocations()

    @Query("DELETE FROM pendingGroupJoinWelcomeEntity")
    suspend fun clearAllPendingGroupJoinWelcomes()

    @Query("DELETE FROM pendingKeyRefreshEntity")
    suspend fun clearAllPendingKeyRefreshes()

    @Query("DELETE FROM archivedGroupDeviceKeyEntity")
    suspend fun clearAllArchivedGroupDeviceKeys()
}