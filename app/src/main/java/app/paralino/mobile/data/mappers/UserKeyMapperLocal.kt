package app.paralino.mobile.data.mappers

import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.mobile.data.models.user.UserKeyEntity

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