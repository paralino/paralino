package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.main.history.PendingLocationHistoryEntity
import app.paralino.core.models.location.LocationHistoryPointModelForMember
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.utils.BaseRemoteModule

class PendingLocationHistoryMapper(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource
) {
    suspend fun pendingLocationToCore(models: List<PendingLocationHistoryEntity>): List<LocationHistoryPointModelForMember> {
        return models
            .groupBy { it.userKeyVersion }
            .flatMap { model ->
                val userKey = userKeyRepo.personalKey(version = model.key)
                model.value.mapNotNull { entry ->
                    try {
                        cryptoCore.decryptDataWithKey(
                            data = entry.locationHistoryData,
                            key = userKey.key
                        ).let {
                            BaseRemoteModule.locationHistoryFormatter.decodeFromString<LocationHistoryPointModel>(it)
                        }.let {
                            LocationHistoryPointModelForMember(
                                groupId = entry.groupId,
                                entryId = entry.id,
                                locationId = entry.locationId,
                                localGroupHistoryKeyId = entry.localGroupHistoryKeyId,
                                history = it
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
}