package app.paralino.core.models.mappers

import app.paralino.core.db.main.privacyzones.PrivacyZoneEntity
import app.paralino.core.models.data
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.models.raw.GroupPrivacyZoneRaw

class GroupPrivacyZoneMapperLocal(
    private val coreMapper: PrivacyZoneMapperCore,
) {

    suspend fun local2Core(model: PrivacyZoneEntity): GroupPrivacyZone {
        return coreMapper.remote2Core(local2Remote(model))
    }

    fun local2Remote(model: PrivacyZoneEntity): GroupPrivacyZoneRaw {
        return GroupPrivacyZoneRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            userKeyVersion = model.userKeyVersion,
            data = model.data,
        )
    }

    suspend fun remote2Local(model: GroupPrivacyZoneRaw): PrivacyZoneEntity {
        val expiresAt = coreMapper.remote2Core(model).data.data?.expiresAt ?: ""
        return PrivacyZoneEntity(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            userKeyVersion = model.userKeyVersion,
            data = model.data,
            expiresAt = expiresAt,
        )
    }
}
