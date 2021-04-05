package com.zbapp.locationtrack.data.models.location

import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.location.models.location.LocationSharingContract
import com.zbapp.locationtrack.data.IDataMapper
import com.zbapp.locationtrack.utils.extensions.toLocalDateTime
import com.zbapp.locationtrack.utils.extensions.toLocalString

object LocationMapper: IDataMapper<LocationEntity, BaseLocationModel> {
    override fun map(model: LocationEntity): BaseLocationModel {
        return BaseLocationModel(
            id = model.id,
            lat = model.lat,
            lng = model.lng,
            accuracy = model.accuracy,
            sessionId = model.sessionId,
            circleId = model.circleId,
            userId = model.userId,
            name = model.name,
            imageBase64 = model.imageBase64,
            timeUTC = model.timeUTC?.toLocalString(),
            batteryLevel = model.batteryLevel,
        )
    }

    override fun mapReversed(model: BaseLocationModel): LocationEntity {
        return LocationEntity(
            id = model.id,
            lat = model.lat,
            lng = model.lng,
            accuracy = model.accuracy,
            sessionId = model.sessionId,
            circleId = model.circleId,
            userId = model.userId,
            name = model.name,
            imageBase64 = model.imageBase64,
            timeUTC = model.timeUTC?.toLocalDateTime(),
            batteryLevel = model.batteryLevel
        )
    }

}

object SharingContractsMapper: IDataMapper<SharingContractEntity, LocationSharingContract> {
    override fun map(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(model.circleId, model.endTimeUTC)
    }

    override fun mapReversed(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(model.circleId, model.endTimeUTC)
    }
}