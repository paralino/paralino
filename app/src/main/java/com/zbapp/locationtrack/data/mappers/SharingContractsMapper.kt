package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.models.mappers.IDataMapperLocal
import com.zbapp.locationtrack.data.models.location.SharingContractEntity

object SharingContractsMapper : IDataMapperLocal<SharingContractEntity, LocationSharingContract> {
    override suspend fun local2Remote(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(
            groupId = model.groupId,
            endTimeUTC = model.endTimeUTC,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            approximateLocationInMeters = model.approximateLocationInMeters
        )
    }

    override suspend fun remote2Local(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(
            groupId = model.groupId,
            endTimeUTC = model.endTimeUTC,
            shareBattery = model.shareBattery,
            shareSpeed = model.shareSpeed,
            shareElevation = model.shareElevation,
            approximateLocationInMeters = model.approximateLocationInMeters
        )
    }
}