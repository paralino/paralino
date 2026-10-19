package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.keys.ArchivedGroupDeviceKeyEntity
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.utils.extensions.nowUTC
import kotlin.coroutines.cancellation.CancellationException

class ArchiveMemberSenderKeyUseCase(
    private val getMemberSenderKey: GetMemberSenderKeyUseCase,
    private val local: CryptoKeyLocalDataSource,
    private val cryptoCore: CryptoCore,
    private val userKeyRepository: CryptoUserKeysLocalSource,
    private val appPrefs: AppPrefs
) {
    /**
     * Must be called before local keys are replaced.
     * @param groupId null when all groups are refreshed
     */
    suspend fun archiveReplacedDeviceKeys(
        newDeviceKeys: List<GroupDeviceCryptoKeyRaw>,
        groupId: String?
    ) {
        try {
            val currentUserId = appPrefs.userId
            val newKeyVersions = newDeviceKeys
                // keys created for this user, not keys user created for others
                .filter { it.memberUserId == currentUserId }
                .associate { Triple(it.groupId, it.ownerUserId, it.locationId) to it.deviceKeyVersion }

            val previousDeviceKeys = local.getGroupDeviceKeysFromDatabase().filter { key ->
                val newKeyVersion = newKeyVersions[Triple(key.groupId, key.ownerUserId, key.locationId)]
                (groupId == null || key.groupId == groupId)
                        && key.memberUserId == currentUserId
                        && newKeyVersion != null
                        && newKeyVersion != key.deviceKeyVersion
            }

            val keysToArchived = if (previousDeviceKeys.isEmpty()) {
                emptyList()
            } else {
                val userKey = userKeyRepository.activePersonalKeyWithVersion()
                val archivedAtMillis = nowUTC().toEpochMilliseconds()

                previousDeviceKeys.mapNotNull { keyRaw ->
                    val senderKey = try {
                        getMemberSenderKey.getMemberSenderKeyForReadWrite(
                            groupId = keyRaw.groupId,
                            memberUserId = keyRaw.ownerUserId,
                            locationId = keyRaw.locationId
                        )
                    } catch (e: Exception) {
                        null
                    }

                    // archive only the key that is being replaced
                    (senderKey as? GroupMemberSenderKey.ValidForReadWrite)
                        ?.takeIf { it.deviceKeyVersion == keyRaw.deviceKeyVersion }
                        ?.let {
                            ArchivedGroupDeviceKeyEntity(
                                groupId = keyRaw.groupId,
                                ownerUserId = keyRaw.ownerUserId,
                                locationId = keyRaw.locationId,
                                memberKeyVersion = it.memberKeyVersion,
                                deviceKeyVersion = it.deviceKeyVersion,
                                userKeyVersion = userKey.version,
                                encryptedSenderKey = cryptoCore.encryptDataWithKey(data = it.key, key = userKey.key),
                                archivedAtMillis = archivedAtMillis
                            )
                        }
                }
            }

            local.archiveGroupDeviceKeys(keys = keysToArchived)

            if (keysToArchived.isNotEmpty()) {
                logg.tag("tag_crypto_key_group_member")
                    .d("Archived ${keysToArchived.size} previous device keys (replaced: ${previousDeviceKeys.size})")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").w("Unable to archive previous device keys: ${e.message}")
        }
    }
}
