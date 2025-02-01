package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserCryptoKeyRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val encryptedPublicKey: String,
    val publicKey: String,
    val oldMasterKeys: String?,
    val loginSalt: String,
    val dataSalt: String?,
    val loginAlg: String,
    val dataAlg: String,
    val schemeVersion: String?,
    val userKeyVersion: Long
) {

    val schemeVersionType: SchemeVersion get() = when(schemeVersion) {
        null,
        SchemeVersion.V1_0.key -> SchemeVersion.V1_0
        SchemeVersion.V2_0.key -> SchemeVersion.V2_0
        else -> SchemeVersion.UNKNOWN(key = schemeVersion)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): UserCryptoKeyRaw {
            return UserCryptoKeyRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                encryptedSymmetricKey = map["encryptedSymmetricKey"] as String,
                encryptedPrivateKey = map["encryptedPrivateKey"] as String,
                encryptedPublicKey = map["encryptedPublicKey"] as String,
                publicKey = map["publicKey"] as String,
                oldMasterKeys = map["oldMasterKeys"] as? String?,
                loginSalt = map["loginSalt"] as String,
                dataSalt = map["dataSalt"] as? String?,
                loginAlg = map["loginAlg"] as String,
                dataAlg = map["dataAlg"] as String,
                schemeVersion = map["schemeVersion"] as? String?,
                userKeyVersion = (map["userKeyVersion"] as Number).toLong()
            )
        }
    }
}

sealed interface SchemeVersion {
    val key: String
    data object V1_0: SchemeVersion {
        override val key: String = "1.0"
    }
    data object V2_0: SchemeVersion {
        override val key: String = "2.0"
    }
    data class UNKNOWN(override val key: String): SchemeVersion
}