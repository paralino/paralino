package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.places.GroupPlaceSettingsData
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class PlaceSettingsMapperCore constructor(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository
) {

    suspend fun remote2Core(model: GroupPlaceSettingRaw): GroupPlaceSettings {
        return GroupPlaceSettings(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            memberId = model.memberId,
            alertData = BaseRemoteModule.formatter.decodeFromString(model.alertData)
        )
    }

    suspend fun core2Remote(model: GroupPlaceSettings): GroupPlaceSettingRaw {
        return GroupPlaceSettingRaw(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            memberId = model.memberId,
            alertData = BaseRemoteModule.formatter.encodeToString(model.alertData)
        )
    }

}