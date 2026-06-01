package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.LocationHistoryKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.keys.GroupDeviceIntegrityEntity
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.repo.base.BaseRepository

class CryptoKEKLocalRepository(
    private val local: CryptoKeyLocalDataSource,
    private val getGroupKey: GetGroupKeyUseCase,
    private val getMemberPublicKey: GetMemberPublicKeyUseCase,
    private val getMemberSenderKey: GetMemberSenderKeyUseCase,
    private val getPersonalGroupKey: GetPersonalGroupKeyUseCase,
    private val getGroupHistoryKey: GetGroupHistoryKeyUseCase,
    private val getPersonalHistoryKey: GetPersonalHistoryKeyUseCase,
    private val appPrefs: AppPrefs,
) : BaseRepository() {

    suspend fun getGroupKey(groupId: String, keyVersion: Long?): GroupMasterKey {
        return getGroupKey.getGroupKey(groupId = groupId, keyVersion = keyVersion)
    }

    suspend fun getGroupKey(
        groupId: String,
        groupOwnerId: String,
        keyVersion: Long?
    ): GroupMasterKey {
        return getGroupKey.getGroupKey(groupId = groupId, groupOwnerId = groupOwnerId, keyVersion = keyVersion)
    }

    suspend fun getAllGroupKeys(groupId: String): List<GroupMasterKey.Valid> {
        return getGroupKey.getAllGroupKeys(groupId = groupId)
    }

    suspend fun getAllEncryptedMembershipPublicKeys(groupId: String): List<String> {
        return local.getMemberKeys().filter { it.groupId == groupId }.map { it.signedMembershipPublicKey }
    }

    suspend fun hasGroupKeyByVersion(groupId: String, groupKeyVersion: Long): Boolean {
        return local.getGroupKeys().any {
            it.groupId == groupId && it.groupKeyVersion == groupKeyVersion
        }
    }

    suspend fun hasDeviceKeyByVersion(locationId: String, deviceKeyVersion: Long): Boolean {
        return local.getGroupDeviceKeys().any {
            it.locationId == locationId && it.deviceKeyVersion == deviceKeyVersion
        }
    }

    suspend fun hasMemberKeyByVersion(memberId: String, memberKeyVersion: Long): Boolean {
        return local.getMemberKeys().any {
            it.id == memberId && it.memberKeyVersion == memberKeyVersion
        }
    }

    suspend fun getMemberSenderKey(
        groupId: String,
        memberUserId: String,
        locationId: String
    ): GroupMemberSenderKey {
        return getMemberSenderKey.getMemberSenderKey(
            groupId = groupId,
            memberUserId = memberUserId,
            locationId = locationId
        )
    }

    suspend fun parseGroupLocationHistoryKey(
        keyRaw: GroupHistoryKeyEntity
    ): LocationHistoryKey {
        return getGroupHistoryKey.parseGroupLocationHistoryKey(
            keyRaw = keyRaw
        )
    }

    suspend fun parsePersonalLocationHistoryKey(
        keyRaw: PersonalHistoryKeyEntity
    ): LocationHistoryKey {
        return getPersonalHistoryKey.parsePersonalLocationHistoryKey(
            keyRaw = keyRaw
        )
    }

    suspend fun getGroupDeviceKeys(): List<GroupDeviceCryptoKeyRaw> {
        return local.getGroupDeviceKeys()
    }

    suspend fun getMemberPublicKey(
        groupId: String,
        memberUserId: String
    ): GroupMemberMainPublicKey {
        return getMemberPublicKey.getMemberPublicKey(
            groupId = groupId,
            memberUserId = memberUserId
        )
    }

    suspend fun getMyPreKeyForGroup(
        groupId: String,
        version: Long?,
        includeArchivedKeys: Boolean = false
    ): UserGroupPersonalKeyPair {
        return getPersonalGroupKey.getMyPreKeyForGroup(
            groupId = groupId,
            version = version,
            includeArchivedKeys = includeArchivedKeys
        )
    }

    suspend fun getAllActiveMemberKeyVersions(groupId: String): List<Long> {
        val groupDeviceKeys = local.getGroupDeviceKeys().filter {
            // group device keys created for me
            it.groupId == groupId && it.memberUserId == appPrefs.userId
        }.map { it.memberKeyVersion }
        val groupKeys = local.getGroupKeys().filter {
            // group master keys created for me
            it.groupId == groupId && it.memberUserId == appPrefs.userId
        }.map { it.memberKeyVersion }
        val memberKeys = local.getMemberKeys().filter {
            // member keys
            it.groupId == groupId && it.ownerUserId == appPrefs.userId
        }.map { it.memberKeyVersion }
        return (groupDeviceKeys + groupKeys + memberKeys).distinct()
    }

    suspend fun updateIntegrity(
        groupId: String,
        integrity: String,
        safetySignature: String,
        userKeyVersion: Long
    ) {
        local.updateGroupDeviceIntegrity(
            groupId = groupId,
            integrity = integrity,
            safetySignature = safetySignature,
            userKeyVersion = userKeyVersion
        )
    }

    suspend fun saveAndActivateGroupHistoryKey(
        groupId: String,
        userKeyVersion: Long,
        historyKeyVersion: String,
        encryptedHistoryKey: String,
    ) {
        local.saveAndActivateGroupHistoryKey(
            groupId = groupId,
            userKeyVersion = userKeyVersion,
            historyKeyVersion = historyKeyVersion,
            encryptedHistoryKey = encryptedHistoryKey,
        )
    }

    suspend fun deactivateGroupHistoryKey(groupId: String) {
        local.deactivateGroupHistoryKey(groupId = groupId)
    }

    suspend fun savePendingGroupHistoryMemberKeys(
        groupId: String,
        keys: List<MemberDeviceKeyRotation.GroupHistoryKeyPayload>
    ) {
        local.savePendingGroupHistoryMemberKeys(groupId = groupId, keys = keys)
    }

    suspend fun getDeviceIntegrityForGroup(groupId: String): GroupDeviceIntegrityEntity? {
        return local.getGroupDeviceIntegrity(groupId)
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        local.deleteAllKeysForGroup(groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        local.deleteKeysForUser(groupId, userId)
    }

    suspend fun clearCachedKeys() {
        local.clearCache()
    }
}
