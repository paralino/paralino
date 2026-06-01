package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.LocationHistoryKey
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg

class GetPersonalHistoryKeyUseCase(
    private val cryptoCore: CryptoCore,
    private val userKeyRepository: CryptoUserKeysLocalSource
) {
    suspend fun parsePersonalLocationHistoryKey(
        keyRaw: PersonalHistoryKeyEntity
    ): LocationHistoryKey {
        if (keyRaw.encryptedHistoryKey.isBlank()) {
            logg.tag("tag_crypto_key_history")
                .w("No private history keys available")
            return LocationHistoryKey.Error.MissingHistoryKey
        }

        val userKey = userKeyRepository.personalKey(version = keyRaw.userKeyVersion)

        val historyKey = try {
            cryptoCore.decryptDataWithKey(
                data = keyRaw.encryptedHistoryKey,
                key = userKey.key
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_history")
                .w("Invalid personal history key (${e.message})")
            return LocationHistoryKey.Error.Invalid
        }

        return LocationHistoryKey.Valid(
            key = historyKey,
            historyKeyVersion = keyRaw.historyKeyVersion,
            memberKeyVersion = userKey.version
        )
    }
}