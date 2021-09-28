package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.crpyto.CryptoAuth
import com.zbapp.locationtrack.data.models.group.UserGroupEntity

class GroupsMapper(private val crypto: CryptoAuth): IDataMapper<UserGroup, UserGroupEntity, SimpleDocumentRaw> {
//    override fun map(model: UserGroupEntity): UserGroup {
//        return crypto.decryptModel(model.data)
//    }
//
//    override fun mapReversed(model: UserGroup): UserGroupEntity {
//        return UserGroupEntity(model.id, crypto.encryptModel(model))
//    }

    override fun core2Local(model: UserGroup): UserGroupEntity {
        return UserGroupEntity(model.id, crypto.encryptModel(model))
    }

    override fun core2Remote(model: UserGroup): SimpleDocumentRaw {
        return SimpleDocumentRaw(model.id, crypto.encryptModel(model))
    }

    override fun local2Core(model: UserGroupEntity): UserGroup {
        return crypto.decryptModel(model.data)
    }

    override fun local2Remote(model: UserGroupEntity): SimpleDocumentRaw {
        return SimpleDocumentRaw(model.id, model.data)
    }

    override fun remote2Core(model: SimpleDocumentRaw): UserGroup {
        return crypto.decryptModel(model.data)
    }

    override fun remote2Local(model: SimpleDocumentRaw): UserGroupEntity {
        return UserGroupEntity(model.id, model.data)
    }
}