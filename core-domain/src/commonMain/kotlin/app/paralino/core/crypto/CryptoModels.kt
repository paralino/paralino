package app.paralino.core.crypto

import kotlinx.serialization.Serializable


@Serializable
class SimpleKeyPairData(
    val privateKey: String,
    val publicKey: String
)

@Serializable
data class UserAuthDerivedKeyV1(
    val masterAuthKey: String,
    val loginKey: String,
    val loginSalt: String,
    val kdfAlg: KDFAlg
)

@Serializable
data class UserAuthDerivedKeyV2(
    val loginKey: String,
    val kdfAlg: KDFAlg
)

data class DerivedPasswords(
    val loginPassword: String,
    val backupPassword: String
)

@Serializable
data class CipherAlg(
    val enc: String,
    val alg: String,
    val payload: String?,
    val data: String
) {
    constructor(
        encodingType: EncodingType,
        algType: CipherAlgType,
        payload: String?,
        data: String
    ) : this(
        enc = encodingType.key,
        // only take first key as main one
        alg = algType.keys.first(),
        payload = payload,
        data = data
    )

    val algorithm: CipherAlgType? get() = CipherAlgType.fromKey(alg)
    val encodingType: EncodingType? get() = EncodingType.fromKey(enc)

    fun encode(skipTagsIfDefault: Boolean): String {
        val tags = buildList {
            if (payload.isNullOrBlank()) {
                if (!skipTagsIfDefault || encodingType != EncodingType.BASE64_URL || algorithm != CipherAlgType.DEFAULT) {
                    add(enc)
                    add(alg)
                }
            } else {
                add(enc)
                add(alg)
                add(payload)
            }
        }

        if (tags.isEmpty()) {
            return data
        }

        val tagsPayload = tags.joinToString(separator = ":")
        return "@:$tagsPayload:@$data"
    }

    companion object {
        fun decode(input: String): CipherAlg {

            if (!input.startsWith("@:")) {
                return default(data = input)
            }

            val index = input.indexOf(":@")

            if (index == -1) {
                return default(data = input)
            }

            val metadata = input.substring(2, index).split(":")

            if (metadata.size < 2) {
                return default(data = input)
            }

            val encoding = metadata[0]
            val alg = metadata[1]
            val payload = metadata.getOrNull(2)
            val data = input.substring(index + 2)
            return CipherAlg(enc = encoding, alg = alg, payload = payload, data = data)
        }

        private fun default(data: String): CipherAlg {
            return CipherAlg(
                encodingType = EncodingType.BASE64_URL,
                algType = CipherAlgType.DEFAULT,
                payload = null,
                data = data
            )
        }
    }
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

enum class CipherAlgType(val keys: List<String>) {
    DEFAULT(keys = listOf("", "xcp")),
    DEFAULT_KEY_DERIVE(keys = listOf("xcp-kd"));

    companion object {
        fun fromKey(key: String): CipherAlgType? {
            if (key == "") return DEFAULT
            return CipherAlgType.entries.firstOrNull { it.keys.contains(key) }
        }
    }
}

enum class KDFType(val key: String) {
    ARGON2ID("a2");

    companion object {
        fun fromKey(key: String): KDFType? {
            if (key == "") return ARGON2ID
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
            if (key == "") return BASE64_URL
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
        object MissingOwnerSignatureKeys : Error()
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

sealed class PersonalSpaceDeviceKey {
    open val key: String? = null

    sealed class Error : PersonalSpaceDeviceKey() {
        object Missing : Error()
        object Invalid : Error()
        object InvalidMainKey : Error()
        object MissingMainKey : Error()
    }

    data class Valid(override val key: String) : PersonalSpaceDeviceKey()
}

sealed class LocationHistoryKey {
    sealed class Error : LocationHistoryKey() {
        object MissingKeys : Error()
        object MissingHistoryKey : Error()
        object Invalid : Error()
        data class InvalidPersonalKeyPair(val reason: UserGroupPersonalKeyPair.Error) : Error()
        data class InvalidOwnerPublicKey(val reason: GroupMemberMainPublicKey.Error) : Error()
    }

    data class Valid(
        val key: String,
        val historyKeyVersion: String,
        val memberKeyVersion: Long
    ) : LocationHistoryKey()
}
