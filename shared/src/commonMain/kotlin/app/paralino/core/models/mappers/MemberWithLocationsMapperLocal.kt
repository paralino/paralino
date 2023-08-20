package app.paralino.core.models.mappers

import app.paralino.core.data.db.MemberEntity
import app.paralino.core.data.db.MemberLocationEntity
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.utils.extensions.safeLet

class MemberWithLocationsMapperLocal(
    private val coreMapper: MemberMapperCore,
) {

    suspend fun local2Core(
        member: MemberEntity,
        memberLocations: List<MemberLocationEntity>,
        groupOwnerId: String
    ): MemberModelWithLocations {
        return coreMapper.remote2Core(local2Remote(member, memberLocations), groupOwnerId)
    }

    fun local2Remote(
        model: MemberEntity,
        memberLocations: List<MemberLocationEntity>,
    ): MemberWithLocationsRaw {

        val member = MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            groupKeyVersion = model.groupKeyVersion,
            data = model.data_,
            verification = model.verification,
            settingsRaw = safeLet(
                model.settings_id,
                model.settings_groupId,
                model.settings_ownerUserId,
                model.settings_integrity
            ) { id, groupId, userId, integrity ->
                GroupSettingsRaw(
                    id = id,
                    groupId = groupId,
                    ownerUserId = userId,
                    integrity = integrity
                )
            },
            locationIds = memberLocations.map { it.id }
        )

        val locations = memberLocations.map { location ->
            MemberLocationRaw(
                id = location.id,
                deviceId = location.deviceId,
                groupKeyVersion = location.groupKeyVersion,
                deviceKeyVersion = location.deviceKeyVersion,
                deviceData = location.deviceData,
                locationData = location.locationData,
                status = location.status
            )
        }

        return MemberWithLocationsRaw(
            member = member,
            locations = locations
        )
    }

    fun remote2Local(
        member: MemberDocumentRaw,
        location: MemberLocationRaw
    ): MemberLocationEntity {
        return MemberLocationEntity(
            id = location.id,
            groupId = member.groupId,
            memberId = member.id,
            userId = member.ownerUserId,
            deviceId = location.deviceId,
            groupKeyVersion = location.groupKeyVersion,
            deviceKeyVersion = location.deviceKeyVersion,
            deviceData = location.deviceData,
            locationData = location.locationData,
            status = location.status
        )
    }

    fun remote2Local(model: MemberDocumentRaw): MemberEntity {
        return MemberEntity(
            id = model.id,
            userId = model.ownerUserId,
            groupId = model.groupId,
            groupKeyVersion = model.groupKeyVersion,
            verification = model.verification,
            settings_id = model.settingsRaw?.id,
            settings_groupId = model.settingsRaw?.groupId,
            settings_ownerUserId = model.settingsRaw?.ownerUserId,
            settings_integrity = model.settingsRaw?.integrity,
            data_ = model.data
        )
    }
}
