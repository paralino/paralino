package app.paralino.core.repo.places

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.places.WifiAnchorEntity
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable

class WifiAnchorDataSource(
    private val database: MainDatabaseHelper,
    private val cryptoCore: CryptoCore,
    private val userKeys: CryptoUserKeysLocalSource,
) {
    fun newId(): String = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)

    suspend fun loadAll(): List<WifiAnchor> {
        return database.wifiAnchorDao.getAll().mapNotNull { entity ->
            try {
                val key = userKeys.personalKey(version = entity.userKeyVersion)
                val data = cryptoCore.decryptDataWithKey(data = entity.anchorData, key = key.key)
                BaseRemoteModule.formatter.decodeFromString<WifiAnchor>(data).copy(id = entity.id)
            } catch (e: Exception) {
                logg.tag("place_activity").w("Drop wifi anchor that can't be read: ${e.message}")
                delete(id = entity.id)
                null
            }
        }
    }

    suspend fun save(anchor: WifiAnchor) {
        val key = userKeys.activePersonalKeyWithVersion()
        database.wifiAnchorDao.upsert(
            WifiAnchorEntity(
                id = anchor.id,
                anchorData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(anchor),
                    key = key.key
                ),
                userKeyVersion = key.version,
                updatedAtMillis = nowUTC().toEpochMilliseconds()
            )
        )
    }

    suspend fun delete(id: String) {
        try {
            database.wifiAnchorDao.delete(id = id)
        } catch (e: Exception) {
            // ignore - will be retried on next load
        }
    }

    @Serializable
    data class WifiAnchor(
        val id: String,
        val bssid: String,
        val samples: List<Sample>,
    )

    @Serializable
    data class Sample(
        val lat: Double,
        val lng: Double,
        val timeMillis: Long,
    )
}
