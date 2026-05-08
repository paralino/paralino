package app.paralino.core.models.mappers

import app.paralino.core.db.main.common.SharingContractEntity
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.location.LocationSharingContract

object SharingContractsMapper {
    fun local2Remote(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(
            groupOrSpaceId = model.groupId.asGroupOrSpaceId(),
            endTimeUTCPreference = model.endTimeUTC,
            endTimeUTCOverride = model.endTimeUTCOverride,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            shareNetwork = model.shareNetwork,
            approximateLocationInMeters = model.approximateLocationInMeters,
            shareDeviceStatus = model.shareDeviceStatus,
            saveToHistory = model.saveToHistory,
            isNotifyOthersOn = model.isNotifyOthersOn,
            sharingDurationPreset = model.sharingDurationPreset,
            isEnabled = model.isEnabled
        )
    }

    fun remote2Local(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(
            groupId = model.groupOrSpaceId.id,
            endTimeUTC = model.endTimeUTCPreference,
            endTimeUTCOverride = model.endTimeUTCOverride,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            shareNetwork = model.shareNetwork,
            approximateLocationInMeters = model.approximateLocationInMeters,
            shareDeviceStatus = model.shareDeviceStatus,
            saveToHistory = model.saveToHistory,
            isNotifyOthersOn = model.isNotifyOthersOn,
            sharingDurationPreset = model.sharingDurationPreset,
            isEnabled = model.isEnabled
        )
    }
}