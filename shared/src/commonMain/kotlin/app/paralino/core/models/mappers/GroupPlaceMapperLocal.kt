package app.paralino.core.models.mappers

import app.paralino.core.data.db.PlaceEntity
import app.paralino.core.data.db.PlaceSettingsEntity
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.mappers.PlaceSettingsMapperCore
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw

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
            keyVersion = model.keyVersion,
            private = model.privatePlace,
            data = model.data_
        )
    }

    fun remote2Local(model: GroupPlaceRaw): PlaceEntity {
        return PlaceEntity(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            privatePlace = model.private,
            data_ = model.data
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