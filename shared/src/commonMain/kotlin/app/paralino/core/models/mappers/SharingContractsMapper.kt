package app.paralino.core.models.mappers

import app.paralino.core.db.main.common.SharingContractEntity
import app.paralino.core.models.location.LocationSharingContract

object SharingContractsMapper {
    fun local2Remote(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(
            groupId = model.groupId,
            endTimeUTC = model.endTimeUTC,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            shareNetwork = model.shareNetwork,
            approximateLocationInMeters = model.approximateLocationInMeters,
            shareDeviceStatus = model.shareDeviceStatus,
            saveToHistory = model.saveToHistory,
            isEnabled = model.isEnabled
        )
    }

    fun remote2Local(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(
            groupId = model.groupId,
            endTimeUTC = model.endTimeUTC,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            shareNetwork = model.shareNetwork,
            approximateLocationInMeters = model.approximateLocationInMeters,
            shareDeviceStatus = model.shareDeviceStatus,
            saveToHistory = model.saveToHistory,
            isEnabled = model.isEnabled
        )
    }
}