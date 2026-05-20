package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.location.PrivacyZoneSharedLocationEntity
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.location.PrivacyZoneSharedLocationPayload
import app.paralino.core.utils.BaseRemoteModule

class PrivacyZoneSharedLocationLocalSource(
    private val database: MainDatabaseHelper,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val cryptoCore: CryptoCore,
) {

    suspend fun load(groupId: String, privacyZoneId: String): PrivacyZoneSharedLocationPayload? {
        val entity = database.privacyZoneSharedLocationDao.getOnce(
            groupId = groupId,
            privacyZoneId = privacyZoneId,
        ) ?: return null
        return decrypt(entity)
    }

    suspend fun save(
        groupId: String,
        privacyZoneId: String,
        payload: PrivacyZoneSharedLocationPayload,
    ) {
        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        val encrypted = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(payload),
            key = userKey.key,
        )
        database.privacyZoneSharedLocationDao.upsert(
            PrivacyZoneSharedLocationEntity(
                groupId = groupId,
                privacyZoneId = privacyZoneId,
                userKeyVersion = userKey.version,
                encryptedLocationData = encrypted,
            )
        )
    }

    suspend fun delete(groupId: String, privacyZoneId: String) {
        database.privacyZoneSharedLocationDao.delete(
            groupId = groupId,
            privacyZoneId = privacyZoneId,
        )
    }

    suspend fun deleteForGroup(groupId: String) {
        database.privacyZoneSharedLocationDao.deleteForGroup(groupId = groupId)
    }

    private suspend fun decrypt(entity: PrivacyZoneSharedLocationEntity): PrivacyZoneSharedLocationPayload? {
        return try {
            val key = userKeyRepo.personalKey(version = entity.userKeyVersion).key
            val json = cryptoCore.decryptDataWithKey(
                data = entity.encryptedLocationData,
                key = key,
            )
            BaseRemoteModule.formatter.decodeFromString<PrivacyZoneSharedLocationPayload>(json)
        } catch (e: Exception) {
            logg.w(
                "privacy_zone_location",
                "Unable to decrypt privacy zone shared location for ${entity.groupId}/${entity.privacyZoneId}",
                e,
            )
            null
        }
    }
}
