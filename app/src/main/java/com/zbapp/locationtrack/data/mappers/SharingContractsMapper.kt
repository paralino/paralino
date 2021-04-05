package com.zbapp.locationtrack.data.mappers

import com.zbapp.location.models.location.LocationSharingContract
import com.zbapp.locationtrack.data.models.location.SharingContractEntity

object SharingContractsMapper: IDataMapper<SharingContractEntity, LocationSharingContract> {
    override fun map(model: SharingContractEntity): LocationSharingContract {
        return LocationSharingContract(model.circleId, model.endTimeUTC)
    }

    override fun mapReversed(model: LocationSharingContract): SharingContractEntity {
        return SharingContractEntity(model.circleId, model.endTimeUTC)
    }
}