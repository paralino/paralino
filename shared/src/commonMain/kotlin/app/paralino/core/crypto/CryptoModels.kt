package app.paralino.core.crypto

import app.paralino.core.models.user.SimpleKeyPairData
import kotlinx.serialization.Serializable


class KeyPair(
    val privateKey: String,
    val publicKey: String
)

data class UserAuthDerivedKey(
    val masterAuthKey: String,
    val loginKey: String,
    val loginSalt: String,
    val kdfAlg: KDFAlg
)

data class DerivedPasswords(
    val loginPassword: String,
    val backupPassword: String
)

data class CipherPayload(
    val enc: String,
    val alg: String,
    val data: String
) {
    val kdfType: CipherAlgType? get() = CipherAlgType.fromKey(alg)
    val encodingType: EncodingType? get() = EncodingType.fromKey(enc)
}

@Serializable
data class KDFAlg(
    val encoding: String,
    val kdf: String,
    val memoryMb: Long,
    val rounds: Long,
    val keyBytes: Int
) {
    val kdfType: KDFType? get() = KDFType.fromKey(kdf)
    val encodingType: EncodingType? get() = EncodingType.fromKey(encoding)
    val memoryBytes: Long get() = memoryMb * 1024 * 1024
}

enum class CipherAlgType(val key: String) {
    XSALSA20POLY1305(key = "xsp");

    companion object {
        fun fromKey(key: String): CipherAlgType? {
            return CipherAlgType.entries.firstOrNull { it.key == key }
        }
    }
}

enum class KDFType(val key: String) {
    ARGON2ID("a2");

    companion object {
        fun fromKey(key: String): KDFType? {
            return KDFType.entries.firstOrNull { it.key == key }
        }
    }
}

enum class AlphabetType(val alphabet: String) {
    SAFE(alphabet = "3456789ABCDEFGHJKLMNPQRTUWYZ"),
    EXTENDED(alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
}

enum class HashType(val bytes: Int) {
    GENERIC_256(32),
    GENERIC_512(64)
}

enum class PasswordHashingMode {
    FAST,
    NORMAL,
    SLOW
}

enum class EncodingType(val key: String) {
    HEX("hex"),
    BASE64("b64"),
    BASE64_URL("b64u");

    companion object {
        fun fromKey(key: String): EncodingType? {
            return EncodingType.entries.firstOrNull { it.key == key }
        }
    }
}

sealed class GroupMemberSenderKey {
    sealed class Error : GroupMemberSenderKey() {
        object MissingKeys : Error()
        object MissingSenderKey : Error()
        object Invalid : Error()
        data class InvalidPersonalKeyPair(val reason: UserGroupPersonalKeyPair.Error) : Error()
        data class InvalidOwnerPublicKey(val reason: GroupMemberMainPublicKey.Error) : Error()
    }

    data class Valid(
        val key: String,
        val deviceKeyVersion: Long,
        val memberKeyVersion: Long
    ) : GroupMemberSenderKey()
}

sealed class GroupMasterKey {
    sealed class Error : GroupMasterKey() {
        object Missing : Error()
        object Invalid : Error()
        data class InvalidPersonalKeyPair(val reason: UserGroupPersonalKeyPair.Error) : Error()
        data class InvalidOwnerPublicKey(val reason: GroupMemberMainPublicKey.Error) : Error()
    }

    data class Valid(val key: String, val version: Long) : GroupMasterKey()
}

sealed class GroupMemberMainPublicKey {
    open val key: String? = null

    sealed class Error : GroupMemberMainPublicKey() {
        object WrongIdentity : Error()
        object InvalidPublicKey : Error()
        object UnverifiedPublicKey : Error()
        object WrongPublicKey : Error()
        object Missing : Error()
        data class InvalidPersonalKeyPair(val reason: UserGroupPersonalKeyPair.Error) : Error()
    }

    data class Valid(
        override val key: String,
        val signedKey: String,
        val memberKeyVersion: Long,
        val membershipKey: String,
        val signedMemberShipKey: String,
        val identityKey: GroupMemberIdentityKey.Valid
    ) : GroupMemberMainPublicKey()
}

sealed class GroupMemberIdentityKey {
    open val key: String? = null

    sealed class Error : GroupMemberIdentityKey() {
        object Missing : Error()
        object Invalid : Error()
    }

    data class Valid(override val key: String) : GroupMemberIdentityKey()
}

sealed class UserGroupPersonalKeyPair {
    open val key: SimpleKeyPairData? = null

    sealed class Error : UserGroupPersonalKeyPair() {
        object Missing : Error()
        object Invalid : Error()
    }

    data class Valid(
        override val key: SimpleKeyPairData,
        val version: Long
    ) : UserGroupPersonalKeyPair()
}
