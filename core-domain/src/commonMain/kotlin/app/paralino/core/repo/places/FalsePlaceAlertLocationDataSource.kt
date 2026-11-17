package app.paralino.core.repo.places

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.places.FalsePlaceAlertLocationEntity
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable

class FalsePlaceAlertLocationDataSource(
    private val database: MainDatabaseHelper,
    private val cryptoCore: CryptoCore,
    private val userKeys: CryptoUserKeysLocalSource,
) {
    suspend fun loadAll(): Map<String, List<FalsePlaceAlertLocation>> {
        return database.falsePlaceAlertLocationDao.getAll().mapNotNull { entity ->
            try {
                val key = userKeys.personalKey(version = entity.userKeyVersion)
                val data = cryptoCore.decryptDataWithKey(data = entity.locationsData, key = key.key)
                entity.placeId to BaseRemoteModule.formatter.decodeFromString<List<FalsePlaceAlertLocation>>(data)
            } catch (e: Exception) {
                logg.tag("place_activity")
                    .w("Drop false locations that can't be read - ${entity.placeId}: ${e.message}")
                try {
                    database.falsePlaceAlertLocationDao.delete(placeId = entity.placeId)
                } catch (e: Exception) {
                    // ignore - will be retried on next load
                }
                null
            }
        }.toMap()
    }

    suspend fun save(
        placeId: String,
        falseLocations: List<FalsePlaceAlertLocation>
    ) {
        if (falseLocations.isEmpty()) {
            database.falsePlaceAlertLocationDao.delete(placeId = placeId)
            return
        }

        val key = userKeys.activePersonalKeyWithVersion()
        database.falsePlaceAlertLocationDao.upsert(
            FalsePlaceAlertLocationEntity(
                placeId = placeId,
                locationsData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(falseLocations),
                    key = key.key
                ),
                userKeyVersion = key.version,
                updatedAtMillis = nowUTC().toEpochMilliseconds()
            )
        )
    }

    @Serializable
    data class FalsePlaceAlertLocation(
        val lat: Double,
        val lng: Double,
        val isEnter: Boolean,
        val lastSeenMillis: Long,
        val hits: Int,
    )
}
