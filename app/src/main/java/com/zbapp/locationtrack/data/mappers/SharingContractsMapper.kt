package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.locationtrack.data.models.location.SharingContractEntity

object SharingContractsMapper: IDataMapperLocal<LocationSharingContract, SharingContractEntity> {
    override fun local2Core(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(model.groupId, model.endTimeUTC)
    }

    override fun core2Local(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(model.groupId, model.endTimeUTC)
    }
}