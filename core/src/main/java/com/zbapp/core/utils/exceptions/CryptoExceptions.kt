package com.zbapp.core.utils.exceptions

open class CryptoException(message: String?, cause: Throwable? = null): Exception(message, cause)

class EncryptionException(message: String?, cause: Throwable? = null): CryptoException(message)
class DecryptionException(message: String, cause: Throwable? = null): CryptoException(message)

open class MissingCryptoKeyException(message: String, cause: Throwable? = null): CryptoException(message, cause)

class MissingGroupKeyException(
    val groupId: String,
    val groupName: String
) : MissingCryptoKeyException("Missing group key for group: $groupName ($groupId)")

class MissingMemberKeyException(
    val groupId: String,
    val groupName: String,
    val memberId: String,
    val memberName: String
) : MissingCryptoKeyException("Missing member key for member $memberName ($memberId) in: $groupName ($groupId)")

class MissingUserKeyException(val keyPair: Boolean) : MissingCryptoKeyException("Missing user ${if(keyPair) "key pair" else "private" } key")
