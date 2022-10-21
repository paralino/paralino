package app.paralino.android.data.mappers

import app.paralino.android.data.models.places.GroupPlaceSettingsEntity
import app.paralino.core.models.mappers.IDataMapperLocal
import app.paralino.core.models.mappers.PlaceSettingsMapperCore
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.raw.GroupPlaceSettingRaw

class GroupPlaceSettingsMapperLocal(
    private val coreMapper: PlaceSettingsMapperCore
) : IDataMapperLocal<GroupPlaceSettingsEntity, GroupPlaceSettingRaw> {

    suspend fun local2Core(model: GroupPlaceSettingsEntity): GroupPlaceSettings {
        return coreMapper.remote2Core(local2Remote(model))
    }

    override suspend fun local2Remote(model: GroupPlaceSettingsEntity): GroupPlaceSettingRaw {
        return GroupPlaceSettingRaw(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            memberId = model.memberId,
            alertData = model.alertData
        )
    }

    override suspend fun remote2Local(model: GroupPlaceSettingRaw): GroupPlaceSettingsEntity {
        return GroupPlaceSettingsEntity(
            id = model.id,
            groupId = model.groupId,
            placeId = model.placeId,
            ownerUserId = model.ownerUserId,
            memberId = model.memberId,
            alertData = model.alertData
        )
    }

}