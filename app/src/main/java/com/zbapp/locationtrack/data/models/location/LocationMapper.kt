package com.zbapp.locationtrack.data.models.location

import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.locationtrack.data.IDataMapper
import com.zbapp.locationtrack.utils.extensions.toDateTime
import com.zbapp.locationtrack.utils.extensions.toMillis

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
            time = model.time?.toMillis(),
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
            time = model.time?.toDateTime(),
            batteryLevel = model.batteryLevel
        )
    }

}