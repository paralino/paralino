package app.paralino.core.models.mappers

import app.paralino.core.data.db.SharingContractEntity
import app.paralino.core.models.location.LocationSharingContract

object SharingContractsMapper {
    fun local2Remote(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(
            groupId = model.groupId,
            endTimeUTC = model.endTimeUTC,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            approximateLocationInMeters = model.approximateLocationInMeters?.toInt()
        )
    }

    fun remote2Local(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(
            groupId = model.groupId,
            endTimeUTC = model.endTimeUTC,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            approximateLocationInMeters = model.approximateLocationInMeters?.toLong()
        )
    }
}