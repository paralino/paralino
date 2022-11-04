package app.paralino.core.utils.exceptions

open class CryptoException(message: String?, cause: Throwable? = null): Exception(message, cause)

class EncryptionException(message: String?, cause: Throwable? = null): CryptoException(message)
class DecryptionException(message: String, cause: Throwable? = null): CryptoException(message)

open class MissingCryptoKeyException(message: String, cause: Throwable? = null): CryptoException(message, cause)

class MissingGroupKeyException constructor(
    val groupId: String,
    val keyVersion: Long? = null
) : MissingCryptoKeyException("Missing group key for group ($groupId), key version $keyVersion")

class MissingMemberKeyException constructor(
    val groupId: String,
    val memberId: String,
    val keyVersion: Long? = null
) : MissingCryptoKeyException("Missing member key for member ($memberId) in: ($groupId) for key version $keyVersion")

class MissingUserKeyException(val localCache: Boolean) : MissingCryptoKeyException("Missing main user key from ${if(localCache) "cache" else "storage" }")
