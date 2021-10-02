package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.repo.group.IGroupCrypto
import com.zbapp.core.repo.group.IUserCrypto
import com.zbapp.core.repo.group.decryptModel
import com.zbapp.core.repo.group.encryptModel
import com.zbapp.locationtrack.data.models.group.UserGroupEntity

class GroupsMapper(
    private val crypto: IGroupCrypto
) : IDataMapper<UserGroup, UserGroupEntity, SimpleDocumentRaw> {

    override suspend fun core2Remote(model: UserGroup): SimpleDocumentRaw {
        return SimpleDocumentRaw(model.id, model.ownerId, crypto.encryptModel(model.id, model))
    }

    override suspend fun remote2Core(model: SimpleDocumentRaw): UserGroup {
        return crypto.decryptModel(model.id, model.data, model.ownerId)
    }

    override fun local2Remote(model: UserGroupEntity): SimpleDocumentRaw {
        return SimpleDocumentRaw(model.id, model.ownerId, model.data)
    }

    override fun remote2Local(model: SimpleDocumentRaw): UserGroupEntity {
        return UserGroupEntity(model.id, model.ownerId, model.data)
    }
}