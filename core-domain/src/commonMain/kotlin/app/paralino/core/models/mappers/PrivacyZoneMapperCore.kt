package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.models.privacyzones.PrivacyZoneData
import app.paralino.core.models.raw.GroupPrivacyZoneRaw
import app.paralino.core.utils.BaseRemoteModule

class PrivacyZoneMapperCore(
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val cryptoCore: CryptoCore,
) {
    suspend fun remote2Core(model: GroupPrivacyZoneRaw): GroupPrivacyZone {
        val encryptionKey = try {
            userKeyRepo.personalKey(version = model.userKeyVersion).key
        } catch (e: Exception) {
            logg.w("privacy_zone", "Error parsing user key: ${e.message}")
            null
        }

        val zoneData = if (encryptionKey != null) {
            try {
                cryptoCore.decryptDataWithKey(
                    data = model.data,
                    key = encryptionKey
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<PrivacyZoneData>(it)
                }.let {
                    CryptoResource.Ready(it)
                }
            } catch (e: Exception) {
                CryptoResource.WrongKeyOrData
            }
        } else {
            CryptoResource.MissingKey
        }

        return GroupPrivacyZone(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            userKeyVersion = model.userKeyVersion,
            data = zoneData,
        )
    }
}
