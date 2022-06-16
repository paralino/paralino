package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.mappers.UserKeyMapperCore
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.locationtrack.data.models.user.UserKeyEntity

class UserKeyMapperLocal(
    private val coreMapper: UserKeyMapperCore
) {

    suspend fun mapKeyPair(model: UserKeyEntity, userKey: String): AsymmetricCryptoKeyData {
        return coreMapper.mapKeyPair(local2Remote(model), userKey)
    }
//
//    fun localToSymmetricKey(model: UserKeyEntity): SymmetricCryptoKeyData {
//        return SymmetricCryptoKeyData(
//            encryptedKey = model.encryptedPrivateKey,
//            version = model.version
//        )
//    }

    fun remote2Local(model: UserCryptoKeyRaw): UserKeyEntity {
        return UserKeyEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedSymmetricKey = model.encryptedSymmetricKey,
            encryptedPrivateKey = model.encryptedPrivateKey,
            publicKey = model.publicKey,
            version = model.version
        )
    }

    fun local2Remote(model: UserKeyEntity): UserCryptoKeyRaw {
        return UserCryptoKeyRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedSymmetricKey = model.encryptedSymmetricKey,
            encryptedPrivateKey = model.encryptedPrivateKey,
            publicKey = model.publicKey,
            version = model.version
        )
    }

}