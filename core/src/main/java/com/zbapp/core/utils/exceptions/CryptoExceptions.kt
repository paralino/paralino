package com.zbapp.core.utils.exceptions

open class CryptoException(message: String?, cause: Throwable? = null): Exception(message, cause)

class EncryptionException(message: String?, cause: Throwable? = null): CryptoException(message)
class DecryptionException(message: String, cause: Throwable? = null): CryptoException(message)

open class MissingCryptoKeyException(message: String, cause: Throwable? = null): CryptoException(message, cause)

class MissingGroupKeyException(
    val groupId: String
) : MissingCryptoKeyException("Missing group key for group ($groupId)")

class MissingMemberKeyException(
    val groupId: String,
    val memberId: String
) : MissingCryptoKeyException("Missing member key for member ($memberId) in: ($groupId)")

class MissingUserKeyException(val keyPair: Boolean) : MissingCryptoKeyException("Missing user ${if(keyPair) "key pair" else "private" } key")
