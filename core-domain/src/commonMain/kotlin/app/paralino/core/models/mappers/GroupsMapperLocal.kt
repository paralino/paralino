package app.paralino.core.models.mappers

import app.paralino.core.db.main.groups.GroupEntity
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.utils.extensions.isoWithOffsetToInstant

class GroupsMapperLocal(
    private val coreMapper: GroupMapperCore
) {

    suspend fun local2Core(model: GroupEntity): UserGroup {
        return coreMapper.remote2Core(local2Remote(model))
    }

    fun local2Remote(model: GroupEntity): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            createdAt = model.createdAt?.isoWithOffsetToInstant(),
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            data = model.data,
            serverIntegrity = model.serverIntegrity,
            schemeVersion = model.schemeVersion,
            planType = model.planType,
            planExpiresAt = model.planExpiresAt?.isoWithOffsetToInstant(),
            provider = model.provider,
            membersLimitOverride = model.membersLimitOverride,
            devicesLimitOverride = model.devicesLimitOverride,
            placesLimitOverride = model.placesLimitOverride,
            fallbackPlanType = model.fallbackPlanType ?: PlanConfig.default.planType
        )
    }

    fun remote2Local(model: GroupDocumentRaw): GroupEntity {
        return GroupEntity(
            id = model.id,
            createdAt = model.createdAt?.toString(),
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            data = model.data,
            serverIntegrity = model.serverIntegrity,
            schemeVersion = model.schemeVersion,
            planType = model.planType,
            planExpiresAt = model.planExpiresAt?.toString(),
            provider = model.provider,
            membersLimitOverride = model.membersLimitOverride,
            devicesLimitOverride = model.devicesLimitOverride,
            placesLimitOverride = model.placesLimitOverride,
            fallbackPlanType = model.fallbackPlanType
        )
    }
}