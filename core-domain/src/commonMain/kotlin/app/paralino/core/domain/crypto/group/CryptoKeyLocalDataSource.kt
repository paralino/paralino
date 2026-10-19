package app.paralino.core.domain.crypto.group

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.history.UnsyncedGroupHistoryKeyEntity
import app.paralino.core.db.main.keys.ArchivedGroupDeviceKeyEntity
import app.paralino.core.db.main.keys.GroupDeviceIntegrityEntity
import app.paralino.core.db.main.keys.GroupDeviceKeysEntity
import app.paralino.core.db.main.keys.GroupKeysEntity
import app.paralino.core.db.main.keys.GroupLocalHistoryKeyEntity
import app.paralino.core.db.main.keys.GroupMembersKeysEntity
import app.paralino.core.db.main.keys.LocalGroupDeviceKeysEntity
import app.paralino.core.db.main.keys.PendingKeyRefreshEntity
import app.paralino.core.db.main.keys.PendingKeyRefreshState
import app.paralino.core.db.main.keys.PendingKeyRefreshType
import app.paralino.core.db.main.keys.PersonalGroupKeyEntity
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.mappers.GroupKeysMapper
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class CryptoKeyLocalDataSource constructor(
    private val database: MainDatabaseHelper,
    private val keysMapper: GroupKeysMapper,
    private val appPrefs: AppPrefs,
    private val appBuildConfig: AppBuildConfig
) {

    private var cachedGroupKeys = MutableStateFlow<List<GroupKeysEntity>>(emptyList())
    private var cachedMemberKeys = MutableStateFlow<List<GroupMembersKeysEntity>>(emptyList())
    private var cachedGroupDeviceKeys = MutableStateFlow<List<GroupDeviceKeysEntity>>(emptyList())
    private var cachedPersonalGroupKeys = MutableStateFlow<List<PersonalGroupKeyEntity>>(emptyList())

    suspend fun getGroupKeys(): List<GroupKeysEntity> {
        if (appBuildConfig.appHost == AppHost.MAIN_APP) {
            return cachedGroupKeys.value.ifEmpty {
                getAllGroupKeys()
                    .also { cachedGroupKeys.value = it }
            }
        } else{
            return getAllGroupKeys()
        }
    }

    suspend fun getMemberKeys(): List<GroupMembersKeysEntity> {
        if (appBuildConfig.appHost == AppHost.MAIN_APP) {
            return cachedMemberKeys.value.ifEmpty {
                getAllMemberKeys()
                    .also { cachedMemberKeys.value = it }
            }
        } else{
            return getAllMemberKeys()
        }
    }

    suspend fun getGroupDeviceKeys(): List<GroupDeviceKeysEntity> {
        if (appBuildConfig.appHost == AppHost.MAIN_APP) {
            return cachedGroupDeviceKeys.value.ifEmpty {
                getAllGroupDeviceKeys()
                    .also { cachedGroupDeviceKeys.value = it }
            }
        } else{
            return getAllGroupDeviceKeys()
        }
    }

    suspend fun getPersonalGroupKeys(
        groupId: String,
        includeArchivedKeys: Boolean
    ): List<PersonalGroupKeyEntity> {
        val archivedKeys = if (includeArchivedKeys) {
            getAllArchivedPersonalGroupKeys(groupId = groupId)
        } else {
            emptyList()
        }

        val activeKeys = if (appBuildConfig.appHost == AppHost.MAIN_APP) {
            cachedPersonalGroupKeys.value.ifEmpty {
                getAllActivePersonalGroupKeys()
                    .also { cachedPersonalGroupKeys.value = it }
            }
        } else {
            getAllActivePersonalGroupKeys()
        }

        val targetKeys = activeKeys + archivedKeys

        return targetKeys.filter { it.groupId == groupId }
    }

    suspend fun getLocalGroupSenderKey(groupId: String): LocalGroupDeviceKeysEntity? {
        return database.groupRelatedKeysDao.getLocalGroupDeviceSenderKey(groupId = groupId)
    }

    suspend fun saveLocalGroupSenderKey(key: LocalGroupDeviceKeysEntity) {
        database.groupRelatedKeysDao.createLocalGroupDeviceSenderKey(key)
    }

    suspend fun getSpecificGroupDeviceKey(
        groupId: String,
        ownerUserId: String,
        memberUserId: String,
        locationId: String
    ): List<GroupDeviceKeysEntity> {
        return database.groupRelatedKeysDao.getSpecificGroupDeviceKey(
            groupId = groupId,
            ownerUserId = ownerUserId,
            memberUserId = memberUserId,
            locationId = locationId
        )
    }

    suspend fun getCurrentDeviceLocationId(groupId: String): String? {
        return database.groupRelatedDao.getSingleLocationForMemberAndDeviceOnce(
            groupId = groupId,
            userId = appPrefs.userId,
            deviceId = appPrefs.deviceId
        )?.id
    }

    suspend fun recreateAllGroupKeys(
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        deviceKeys: List<GroupDeviceCryptoKeyRaw>,
        personalKeys: List<PersonalGroupKeyRaw>,
        refreshStartedAt: Instant
    ) {
        val mappedGroupKeys = groupKeys.map { keysMapper.remote2Local(it) }
        val mappedMemberKeys = memberKeys.map { keysMapper.remote2Local(it) }
        val mappedPersonalKeys = personalKeys.map { keysMapper.remote2Local(it) }
        val mappedDeviceKeys = deviceKeys.map { keysMapper.remote2Local(it) }

        cachedGroupKeys.value = mappedGroupKeys
        cachedMemberKeys.value = mappedMemberKeys
        cachedGroupDeviceKeys.value = mappedDeviceKeys
        cachedPersonalGroupKeys.value = mappedPersonalKeys

        database.groupRelatedKeysDao.recreateAllKeys(
            groupKeys = mappedGroupKeys,
            memberKeys = mappedMemberKeys,
            deviceKeys = mappedDeviceKeys,
            personalKeys = mappedPersonalKeys
        )
        database.groupRelatedKeysDao.clearAllPendingKeyRefreshes(
            refreshStartedAtMillis = refreshStartedAt.toEpochMilliseconds(),
            staleStartedBeforeMillis = staleRotationStartedBeforeMillis()
        )
    }

    suspend fun recreateGroupKeyForGroup(
        groupId: String,
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        deviceKeys: List<GroupDeviceCryptoKeyRaw>,
        personalKeys: List<PersonalGroupKeyRaw>,
        refreshStartedAt: Instant
    ) {
        cachedGroupKeys.value = emptyList()
        cachedMemberKeys.value = emptyList()
        cachedGroupDeviceKeys.value = emptyList()
        cachedPersonalGroupKeys.value = emptyList()

        database.groupRelatedKeysDao.recreateAllKeys(
            groupId = groupId,
            groupKeys = groupKeys.map { keysMapper.remote2Local(it) },
            memberKeys = memberKeys.map { keysMapper.remote2Local(it) },
            deviceKeys = deviceKeys.map { keysMapper.remote2Local(it) },
            personalKeys = personalKeys.map { keysMapper.remote2Local(it) }
        )
        clearPendingKeyRefreshes(
            groupId = groupId,
            keyTypes = PendingKeyRefreshType.entries,
            refreshStartedAt = refreshStartedAt
        )
    }

    suspend fun recreateGroupKeyForGroup(
        groupId: String,
        keys: List<GroupCryptoKeyDataRaw>,
        refreshStartedAt: Instant
    ) {
        cachedGroupKeys.value = emptyList()
        database.groupRelatedKeysDao.recreateGroupKeys(
            groupId = groupId,
            groupKeys = keys.map { keysMapper.remote2Local(it) }
        )
        clearPendingKeyRefreshes(
            groupId = groupId,
            keyTypes = listOf(PendingKeyRefreshType.GROUP),
            refreshStartedAt = refreshStartedAt
        )
    }

    suspend fun recreateMemberKeysForGroup(
        groupId: String,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        personalKeys: List<PersonalGroupKeyRaw>,
        refreshStartedAt: Instant
    ) {
        cachedMemberKeys.value = emptyList()
        cachedPersonalGroupKeys.value = emptyList()

        database.groupRelatedKeysDao.recreateMemberKeys(
            groupId = groupId,
            memberKeys = memberKeys.map { keysMapper.remote2Local(it) },
            personalKeys = personalKeys.map { keysMapper.remote2Local(it) }
        )
        clearPendingKeyRefreshes(
            groupId = groupId,
            keyTypes = listOf(PendingKeyRefreshType.MEMBER),
            refreshStartedAt = refreshStartedAt
        )
    }

    suspend fun recreateDeviceKeysForGroup(
        groupId: String,
        deviceKeys: List<GroupDeviceCryptoKeyRaw>,
        refreshStartedAt: Instant
    ) {
        cachedGroupDeviceKeys.value = emptyList()
        database.groupRelatedKeysDao.recreateDeviceKeys(
            groupId = groupId,
            deviceKeys = deviceKeys.map { keysMapper.remote2Local(it) }
        )
        clearPendingKeyRefreshes(
            groupId = groupId,
            keyTypes = listOf(PendingKeyRefreshType.DEVICE),
            refreshStartedAt = refreshStartedAt
        )
    }

    suspend fun markKeyRotationStarted(groupId: String, keyType: PendingKeyRefreshType) {
        savePendingKeyRefresh(groupId = groupId, keyType = keyType, state = PendingKeyRefreshState.STARTED)
    }

    suspend fun markKeyRotationDone(groupId: String, keyType: PendingKeyRefreshType) {
        savePendingKeyRefresh(groupId = groupId, keyType = keyType, state = PendingKeyRefreshState.DONE)
    }

    suspend fun hasPendingKeyRefresh(groupId: String): Boolean {
        return database.groupRelatedKeysDao.countPendingKeyRefreshes(
            groupId = groupId,
            staleStartedBeforeMillis = staleRotationStartedBeforeMillis()
        ) > 0
    }

    private suspend fun savePendingKeyRefresh(
        groupId: String,
        keyType: PendingKeyRefreshType,
        state: PendingKeyRefreshState
    ) {
        database.groupRelatedKeysDao.savePendingKeyRefresh(
            PendingKeyRefreshEntity(
                groupId = groupId,
                keyType = keyType.key,
                state = state.key,
                updatedAtMillis = nowUTC().toEpochMilliseconds()
            )
        )
    }

    private suspend fun clearPendingKeyRefreshes(
        groupId: String,
        keyTypes: List<PendingKeyRefreshType>,
        refreshStartedAt: Instant
    ) {
        database.groupRelatedKeysDao.clearPendingKeyRefreshes(
            groupId = groupId,
            keyTypes = keyTypes.map { it.key },
            refreshStartedAtMillis = refreshStartedAt.toEpochMilliseconds(),
            staleStartedBeforeMillis = staleRotationStartedBeforeMillis()
        )
    }

    private fun staleRotationStartedBeforeMillis(): Long {
        // if rotation started more than 5 minutes ago, but not done yet, treat it as done
        return (nowUTC() - 5.minutes).toEpochMilliseconds()
    }

    private fun archivedDeviceKeysExpiredBeforeMillis(): Long {
        // previous device keys are kept for 7 days after rotation
        return (nowUTC() - 7.days).toEpochMilliseconds()
    }

    suspend fun getGroupDeviceKeysFromDatabase(): List<GroupDeviceKeysEntity> {
        return getAllGroupDeviceKeys()
    }

    suspend fun archiveGroupDeviceKeys(keys: List<ArchivedGroupDeviceKeyEntity>) {
        database.groupRelatedKeysDao.archiveGroupDeviceKeys(
            keys = keys,
            removeExpiredBeforeMillis = archivedDeviceKeysExpiredBeforeMillis()
        )
    }

    suspend fun getArchivedGroupDeviceKey(
        groupId: String,
        ownerUserId: String,
        locationId: String
    ): ArchivedGroupDeviceKeyEntity? {
        return database.groupRelatedKeysDao.getArchivedGroupDeviceKey(
            groupId = groupId,
            ownerUserId = ownerUserId,
            locationId = locationId,
            notExpiredAfterMillis = archivedDeviceKeysExpiredBeforeMillis()
        )
    }

    suspend fun getGroupDeviceIntegrity(groupId: String): GroupDeviceIntegrityEntity? {
        return database.groupRelatedKeysDao.getGroupDeviceIntegrityKey(groupId)
    }

    suspend fun updateGroupDeviceIntegrity(
        groupId: String,
        integrity: String,
        safetySignature: String,
        userKeyVersion: Long
    ) {
        database.groupRelatedKeysDao.createGroupDeviceIntegrityKey(
            GroupDeviceIntegrityEntity(
                id = groupId,
                integrity = integrity,
                safetySignature = safetySignature,
                userKeyVersion = userKeyVersion
            )
        )
    }

    suspend fun saveAndActivateGroupHistoryKey(
        groupId: String,
        userKeyVersion: Long,
        historyKeyVersion: String,
        encryptedHistoryKey: String
    ) {
        database.groupLocalHistoryKeyDao.saveAndActivateHistoryKey(
            key = GroupLocalHistoryKeyEntity(
                groupId = groupId,
                createdAt = nowUTC().toString(),
                userKeyVersion = userKeyVersion,
                historyKeyVersion = historyKeyVersion,
                encryptedHistoryKey = encryptedHistoryKey,
                isActive = true
            )
        )
    }

    suspend fun deactivateGroupHistoryKey(groupId: String) {
        database.groupLocalHistoryKeyDao.markKeysAsInactive(groupId)
    }

    suspend fun savePendingGroupHistoryMemberKeys(
        groupId: String,
        keys: List<MemberDeviceKeyRotation.GroupHistoryKeyPayload>
    ) {
        database.unsyncedGroupHistoryKeysDao.saveLocalHistoryKeys(
            keys = keys.map { key ->
                UnsyncedGroupHistoryKeyEntity(
                    id = 0,
                    groupId = groupId,
                    memberUserId = key.userId,
                    memberId = key.memberId,
                    memberVerification = key.memberVerification,
                    memberKeyVersion = key.memberKeyVersion,
                    historyKeyVersion = key.historyKeyVersion,
                    encryptedHistoryKey = key.encryptedHistoryKey,
                    isSyncedOnce = false
                )
            }
        )
    }

    private suspend fun getAllGroupKeys(): List<GroupKeysEntity> {
        return database.groupRelatedKeysDao.getAllGroupKeys()
    }

    private suspend fun getAllMemberKeys(): List<GroupMembersKeysEntity> {
        return database.groupRelatedKeysDao.getAllGroupMembersKeys()
    }

    private suspend fun getAllGroupDeviceKeys(): List<GroupDeviceKeysEntity> {
        return database.groupRelatedKeysDao.getAllGroupDeviceKeys()
    }

    private suspend fun getAllArchivedPersonalGroupKeys(groupId: String): List<PersonalGroupKeyEntity> {
        return database.groupRelatedKeysDao.getAllArchivedPersonalGroupKeys(groupId = groupId)
    }

    private suspend fun getAllActivePersonalGroupKeys(): List<PersonalGroupKeyEntity> {
        return database.groupRelatedKeysDao.getAllActivePersonalGroupKeys()
    }

    suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupRelatedDao.getGroupOwnerUserId(groupId)
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        cachedGroupKeys.value = cachedGroupKeys.value.filter { it.groupId != groupId }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.groupId != groupId }
        cachedGroupDeviceKeys.value = cachedGroupDeviceKeys.value.filter { it.groupId != groupId }
        cachedPersonalGroupKeys.value = cachedPersonalGroupKeys.value.filter { it.groupId != groupId }

        database.groupRelatedKeysDao.clearAllGroupRelatedKeys(groupId = groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        cachedGroupDeviceKeys.value = cachedGroupDeviceKeys.value.filter {
            // remove all keys that were either created by or for userId
            !(it.memberUserId == userId || it.ownerUserId == userId)
        }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.ownerUserId != userId }

        database.groupRelatedKeysDao.clearAllKeysForUser(groupId = groupId, userId = userId)
    }

    suspend fun clearCache() {
        cachedMemberKeys.value = emptyList()
        cachedGroupKeys.value = emptyList()
        cachedGroupDeviceKeys.value = emptyList()
        cachedPersonalGroupKeys.value = emptyList()
    }
}