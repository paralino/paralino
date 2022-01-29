package com.zbapp.crpyto

import com.cossacklabs.themis.KeypairGenerator
import com.cossacklabs.themis.SymmetricKey
import com.zbapp.core.config
import com.zbapp.core.crypto.*

class CryptoKeyProvider: ICryptoKeyProvider {

    override fun createNewSymmetricKey(): Data {
        return SymmetricKey().toByteArray().toData()
    }

    override fun createNewAsymmetricKey(): KeyPair {
        val key = KeypairGenerator.generateKeypair()
        return KeyPair(key.privateKey.toByteArray(), key.publicKey.toByteArray())
    }
}