package app.paralino.android.data.mappers

import app.paralino.android.data.models.location.MemberEntity
import app.paralino.android.data.models.location.MemberEntityWithLocations
import app.paralino.android.data.models.location.MemberLocationEntity
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.mappers.IDataMapperLocal
import app.paralino.core.models.mappers.MemberMapperCore
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw

class MemberWithLocationsMapperLocal(
    private val coreMapper: MemberMapperCore,
    val memberMapper: MemberMapperLocal
) : IDataMapperLocal<MemberEntityWithLocations, MemberWithLocationsRaw> {

    suspend fun local2Core(
        model: MemberEntityWithLocations,
        groupOwnerId: String
    ): MemberModelWithLocations {
        return coreMapper.remote2Core(local2Remote(model), groupOwnerId)
    }

    override suspend fun local2Remote(model: MemberEntityWithLocations): MemberWithLocationsRaw {

        val member = memberMapper.local2Remote(model.member)

        val locations = model.locations.map { location ->
            MemberLocationRaw(
                id = location.id,
                deviceId = location.deviceId,
                deviceData = location.deviceData,
                locationData = location.locationData
            )
        }

        return MemberWithLocationsRaw(
            member = member,
            locations = locations
        )
    }

    override suspend fun remote2Local(model: MemberWithLocationsRaw): MemberEntityWithLocations {

        val member = memberMapper.remote2Local(model.member)

        val locations = model.locations.map { location ->
            MemberLocationEntity(
                id = location.id,
                groupId = model.member.groupId,
                memberId = model.member.id,
                userId = model.member.ownerUserId,
                deviceId = location.deviceId,
                deviceData = location.deviceData,
                locationData = location.locationData
            )
        }

        return MemberEntityWithLocations(
            member = member,
            locations = locations
        )
    }

}

class MemberMapperLocal() : IDataMapperLocal<MemberEntity, MemberDocumentRaw> {

    override suspend fun local2Remote(model: MemberEntity): MemberDocumentRaw {
        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            groupKeyVersion = model.groupKeyVersion,
            memberKeyVersion = model.memberKeyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            data = model.data,
            verification = model.verification,
            integrity = model.integrity,
            locationIds = emptyList()
        )
    }

    override suspend fun remote2Local(model: MemberDocumentRaw): MemberEntity {
        return MemberEntity(
            id = model.id,
            userId = model.ownerUserId,
            groupId = model.groupId,
            groupKeyVersion = model.groupKeyVersion,
            memberKeyVersion = model.memberKeyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            verification = model.verification,
            integrity = model.integrity,
            data = model.data
        )
    }

}
