package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.location.ApproximateSharedLocationEntity
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.ApproximateSharedLocationPayload
import app.paralino.core.utils.BaseRemoteModule

class ApproximateSharedLocationLocalSource(
    private val database: MainDatabaseHelper,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val cryptoCore: CryptoCore,
) {

    suspend fun load(groupId: GroupOrSpaceId): ApproximateSharedLocationPayload? {
        val entity = database.approximateSharedLocationDao.getOnce(groupId = groupId.id) ?: return null
        return decrypt(entity)
    }

    suspend fun save(groupId: GroupOrSpaceId, payload: ApproximateSharedLocationPayload) {
        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        val encrypted = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(payload),
            key = userKey.key,
        )
        database.approximateSharedLocationDao.upsert(
            ApproximateSharedLocationEntity(
                groupId = groupId.id,
                userKeyVersion = userKey.version,
                encryptedLocationData = encrypted,
            )
        )
    }

    suspend fun delete(groupId: GroupOrSpaceId) {
        database.approximateSharedLocationDao.delete(groupId = groupId.id)
    }

    private suspend fun decrypt(entity: ApproximateSharedLocationEntity): ApproximateSharedLocationPayload? {
        return try {
            val key = userKeyRepo.personalKey(version = entity.userKeyVersion).key
            val json = cryptoCore.decryptDataWithKey(
                data = entity.encryptedLocationData,
                key = key,
            )
            BaseRemoteModule.formatter.decodeFromString<ApproximateSharedLocationPayload>(json)
        } catch (e: Exception) {
            logg.w("approximate_location", "Unable to decrypt approximate shared location for ${entity.groupId}", e)
            null
        }
    }
}
