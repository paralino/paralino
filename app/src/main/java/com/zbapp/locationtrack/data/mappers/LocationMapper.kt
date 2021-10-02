package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.repo.group.IGroupCrypto
import com.zbapp.core.repo.group.decryptModel
import com.zbapp.core.repo.group.encryptModel
import com.zbapp.locationtrack.data.models.location.LocationEntity

class LocationMapper(
    private val crypto: IGroupCrypto
) : IDataMapper<BaseLocationModel, LocationEntity, SimpleDocumentRaw> {

    override suspend fun core2Remote(model: BaseLocationModel): SimpleDocumentRaw {
        return SimpleDocumentRaw(model.id, model.userId, crypto.encryptModel(model.id, model))
    }

    override fun local2Remote(model: LocationEntity): SimpleDocumentRaw {
        return SimpleDocumentRaw(model.id, model.userId, model.data)
    }

    override suspend fun remote2Core(model: SimpleDocumentRaw): BaseLocationModel {
        return crypto.decryptModel(model.id, model.data, model.ownerId)
    }

    override fun remote2Local(model: SimpleDocumentRaw): LocationEntity {
        return LocationEntity(model.id, model.ownerId ?: "", model.data)
    }

}
