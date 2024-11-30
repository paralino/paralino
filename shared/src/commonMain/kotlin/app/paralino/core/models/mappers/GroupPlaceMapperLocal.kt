package app.paralino.core.models.mappers

import app.paralino.core.db.main.places.PlaceEntity
import app.paralino.core.db.main.places.PlaceSettingsEntity
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

class GroupPlaceMapperLocal(
    private val coreMapper: PlaceMapperCore,
    private val settingsCoreMapper: PlaceSettingsMapperCore
) {

    suspend fun local2Core(model: PlaceEntity): GroupPlace {
        return coreMapper.remote2Core(local2Remote(model))
    }

    fun local2Remote(model: PlaceEntity): GroupPlaceRaw {
        return GroupPlaceRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            private = model.privatePlace,
            data = model.data,
            eventKeys = try {
                BaseRemoteModule.formatter.decodeFromString(model.eventKeys)
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    fun remote2Local(model: GroupPlaceRaw): PlaceEntity {
        return PlaceEntity(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            privatePlace = model.private,
            data = model.data,
            eventKeys = BaseRemoteModule.formatter.encodeToString(model.eventKeys)
        )
    }

    suspend fun local2Core(model: PlaceSettingsEntity): GroupPlaceSettings {
        return settingsCoreMapper.remote2Core(local2Remote(model))
    }

    fun local2Remote(model: PlaceSettingsEntity): GroupPlaceSettingRaw {
        return GroupPlaceSettingRaw(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            alertData = model.alertData
        )
    }

    fun remote2Local(model: GroupPlaceSettingRaw): PlaceSettingsEntity {
        return PlaceSettingsEntity(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            alertData = model.alertData
        )
    }
}