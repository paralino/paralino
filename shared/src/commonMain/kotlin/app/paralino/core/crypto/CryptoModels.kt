package app.paralino.core.crypto

import app.paralino.core.models.user.SimpleKeyPairData


class KeyPair(
    val privateKey: String,
    val publicKey: String
)

data class DerivedPasswords(
    val loginPassword: String,
    val backupPassword: String
)

sealed class GroupMemberSenderKey {
    sealed class Error: GroupMemberSenderKey() {
        object MissingKeys: Error()
        object MissingSenderKey: Error()
        object Invalid: Error()
        data class InvalidPersonalKeyPair(val reason: UserGroupPersonalKeyPair.Error): Error()
        data class InvalidOwnerPublicKey(val reason: GroupMemberMainPublicKey.Error): Error()
    }
    data class Valid(val key: String, val version: Long?): GroupMemberSenderKey()
}

sealed class GroupMasterKey {
    sealed class Error: GroupMasterKey() {
        object Missing: Error()
        object Invalid: Error()
        data class InvalidPersonalKeyPair(val reason: UserGroupPersonalKeyPair.Error): Error()
        data class InvalidOwnerPublicKey(val reason: GroupMemberMainPublicKey.Error): Error()
    }
    data class Valid(val key: String, val version: Long): GroupMasterKey()
}

sealed class GroupMemberMainPublicKey {
    open val key: String? = null

    sealed class Error: GroupMemberMainPublicKey() {
        object WrongIdentity: Error()
        object InvalidPublicKey: Error()
        object UnverifiedPublicKey: Error()
        object WrongPublicKey: Error()
        object Missing: Error()
        data class InvalidPersonalKeyPair(val reason: UserGroupPersonalKeyPair.Error): Error()
    }

    data class Valid(
        override val key: String,
        val signedKey: String,
        val identityKey: GroupMemberIdentityKey.Valid
    ) : GroupMemberMainPublicKey()
}

sealed class GroupMemberIdentityKey {
    open val key: String? = null

    sealed class Error: GroupMemberIdentityKey() {
        object Missing: Error()
        object Invalid: Error()
    }
    data class Valid(override val key: String): GroupMemberIdentityKey()
}

sealed class UserGroupPersonalKeyPair {
    open val key: SimpleKeyPairData? = null

    sealed class Error: UserGroupPersonalKeyPair() {
        object Missing: Error()
        object Invalid: Error()
    }
    data class Valid(override val key: SimpleKeyPairData): UserGroupPersonalKeyPair()
}
