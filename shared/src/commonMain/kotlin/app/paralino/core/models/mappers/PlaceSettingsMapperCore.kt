package app.paralino.core.models.mappers

import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

class PlaceSettingsMapperCore {

    suspend fun remote2Core(model: GroupPlaceSettingRaw): GroupPlaceSettings {
        return GroupPlaceSettings(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            deviceId = model.deviceId,
            alertData = try {
                BaseRemoteModule.formatter.decodeFromString(model.alertData)
            } catch (e: Exception) {
                null
            }
        )
    }

    suspend fun core2Remote(model: GroupPlaceSettings): GroupPlaceSettingRaw {
        return GroupPlaceSettingRaw(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            deviceId = model.deviceId,
            alertData = model.alertData?.let { BaseRemoteModule.formatter.encodeToString(it) } ?: ""
        )
    }
}