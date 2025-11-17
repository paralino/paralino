package app.paralino.core.utils.exceptions

open class CryptoException(message: String?, cause: Throwable? = null): Exception(message, cause)

class EncryptionException(message: String?, cause: Throwable? = null): CryptoException(message)
class DecryptionException(
    val type: String,
    message: String,
    cause: Throwable? = null
): CryptoException(message, cause) {
    override fun toString(): String {
        return "${super.toString()}, type: $type, message: $message"
    }
}

open class MissingCryptoKeyException(message: String, cause: Throwable? = null): CryptoException(message, cause)

class MissingGroupKeyException constructor(
    val groupId: String,
    val keyVersion: Long? = null,
    val keyError: String? = null
) : MissingCryptoKeyException("Missing group key${if (keyError != null) " ($keyError)" else " "} for group ($groupId), key version $keyVersion")

class MissingMemberKeyException constructor(
    val groupId: String,
    val memberId: String,
    val keyVersion: Long? = null,
    val keyError: String? = null
) : MissingCryptoKeyException("Missing member key${if (keyError != null) " ($keyError)" else " "} for member ($memberId) in: ($groupId) for key version $keyVersion")

class MissingUserKeyException(val localCache: Boolean) : MissingCryptoKeyException("Missing main user key from ${if(localCache) "cache" else "storage" }")

class UnsupportedGroupEncryptionSchemeException(val groupId: String, val schemeVersion: String): CryptoException("Encryption scheme $schemeVersion in group $groupId not supported")
class UnsupportedEncryptionSchemeException(val scheme: String): CryptoException("Encryption scheme $scheme not supported")
class DeviceSafetySignatureCorrupted(val groupId: String): CryptoException("Device safety signature corrupted for group $groupId")
