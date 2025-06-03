package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.main.location.PendingLocationHistoryEntity
import app.paralino.core.models.location.LocationHistoryModelForMember
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.utils.BaseRemoteModule

class PersonalLocationHistoryMapper(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource
) {
    suspend fun pendingLocationToCore(models: List<PendingLocationHistoryEntity>): List<LocationHistoryModelForMember> {
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
                            LocationHistoryModelForMember(
                                groupId = entry.groupId,
                                entryId = entry.id,
                                localHistoryKeyId = entry.localHistoryKeyId,
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