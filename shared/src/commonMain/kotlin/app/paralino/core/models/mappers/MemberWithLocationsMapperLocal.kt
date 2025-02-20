package app.paralino.core.models.mappers

import app.paralino.core.db.main.groups.MemberEntity
import app.paralino.core.db.main.groups.MemberLocationEntity
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.utils.extensions.safeLet

class MemberWithLocationsMapperLocal(
    val coreMapper: MemberMapperCore,
) {

    suspend fun local2Core(
        member: MemberEntity,
        memberLocations: List<MemberLocationEntity>,
        group: UserGroup
    ): MemberModelWithLocations {
        return coreMapper.remote2Core(local2Remote(member, memberLocations), group)
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
            memberKeyVersion = model.memberKeyVersion,
            data = model.data,
            paralinoId = model.paralinoId,
            verification = model.verification,
            settingsRaw = safeLet(
                model.settingsId,
                model.settingsGroupId,
                model.settingsOwnerUserId,
                model.settingsMemberIntegrity,
                model.settingsMemberIntegrityUserKeyVersion
            ) { id, groupId, userId, memberIntegrity, memberIntegrityUserKeyVersion ->
                GroupSettingsRaw(
                    id = id,
                    groupId = groupId,
                    ownerUserId = userId,
                    memberIntegrity = memberIntegrity,
                    groupIntegrity = model.settingsGroupIntegrity,
                    memberIntegrityUserKeyVersion = memberIntegrityUserKeyVersion,
                    groupIntegrityUserKeyVersion = model.settingsGroupIntegrityUserKeyVersion,
                    batteryAlertSettings = model.settingsBatteryAlertSettings
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
            memberKeyVersion = model.memberKeyVersion,
            verification = model.verification,
            settingsId = model.settingsRaw?.id,
            settingsGroupId = model.settingsRaw?.groupId,
            settingsOwnerUserId = model.settingsRaw?.ownerUserId,
            settingsMemberIntegrity = model.settingsRaw?.memberIntegrity,
            settingsGroupIntegrity = model.settingsRaw?.groupIntegrity,
            settingsMemberIntegrityUserKeyVersion = model.settingsRaw?.memberIntegrityUserKeyVersion,
            settingsGroupIntegrityUserKeyVersion = model.settingsRaw?.groupIntegrityUserKeyVersion,
            settingsBatteryAlertSettings = model.settingsRaw?.batteryAlertSettings,
            data = model.data,
            paralinoId = model.paralinoId,
        )
    }
}
