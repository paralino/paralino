package com.zbapp.locationtrack.data.mappers

import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.locationtrack.data.models.location.LocationEntity
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
