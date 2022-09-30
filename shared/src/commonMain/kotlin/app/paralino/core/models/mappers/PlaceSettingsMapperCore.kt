package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.places.GroupPlaceSettingsData
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString

class PlaceSettingsMapperCore constructor(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository
) {

    suspend fun remote2Core(model: GroupPlaceSettingRaw): GroupPlaceSettings {

        val alertData = cryptoCore.decryptDataWithKey(
            data = model.alertData,
            key = userKeyRepo.personalKey.content
        ).let {
            BaseRemoteModule.formatter.decodeFromString<List<GroupPlaceSettingsData>>(it)
        }

        return GroupPlaceSettings(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            memberId = model.memberId,
            alertData = alertData
        )
    }

    suspend fun core2Remote(model: GroupPlaceSettings): GroupPlaceSettingRaw {

        val alertData = cryptoCore.encryptModel(
            data = model.alertData,
            key = userKeyRepo.personalKey.content
        )

        return GroupPlaceSettingRaw(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            memberId = model.memberId,
            alertData = alertData
        )
    }

}