package app.paralino.core.models.mappers

import app.paralino.core.data.db.GroupEntity
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.GroupDocumentRaw

class GroupsMapperLocal(
    private val coreMapper: GroupMapperCore
) {

    suspend fun local2Core(model: GroupEntity): UserGroup {
        return coreMapper.remote2Core(local2Remote(model))
    }

    fun local2Remote(model: GroupEntity): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            data = model.data_,
            serverIntegrity = model.serverIntegrity,
            planType = model.planType,
            schemeVersion = model.schemeVersion,
            membersLimitOverride = model.membersLimitOverride,
            devicesLimitOverride = model.devicesLimitOverride,
            placesLimitOverride = model.placesLimitOverride
        )
    }

    fun remote2Local(model: GroupDocumentRaw): GroupEntity {
        return GroupEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            data_ = model.data,
            serverIntegrity = model.serverIntegrity,
            planType = model.planType,
            schemeVersion = model.schemeVersion,
            membersLimitOverride = model.membersLimitOverride,
            devicesLimitOverride = model.devicesLimitOverride,
            placesLimitOverride = model.placesLimitOverride
        )
    }
}