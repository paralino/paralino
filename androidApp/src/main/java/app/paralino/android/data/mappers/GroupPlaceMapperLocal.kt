package app.paralino.android.data.mappers

import app.paralino.android.data.models.places.GroupPlaceEntity
import app.paralino.core.models.mappers.IDataMapperLocal
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.raw.GroupPlaceRaw

class GroupPlaceMapperLocal(
    private val coreMapper: PlaceMapperCore
) : IDataMapperLocal<GroupPlaceEntity, GroupPlaceRaw> {

    suspend fun local2Core(model: GroupPlaceEntity): GroupPlace {
        return coreMapper.remote2Core(local2Remote(model))
    }

    override suspend fun local2Remote(model: GroupPlaceEntity): GroupPlaceRaw {
        return GroupPlaceRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            private = model.privatePlace,
            data = model.data
        )
    }

    override suspend fun remote2Local(model: GroupPlaceRaw): GroupPlaceEntity {
        return GroupPlaceEntity(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            privatePlace = model.private,
            data = model.data
        )
    }

}