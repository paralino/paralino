package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.locationtrack.data.models.location.SharingContractEntity

object SharingContractsMapper: IDataMapper<SharingContractEntity, LocationSharingContract> {
    override fun map(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(model.groupId, model.endTimeUTC)
    }

    override fun mapReversed(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(model.groupId, model.endTimeUTC)
    }
}