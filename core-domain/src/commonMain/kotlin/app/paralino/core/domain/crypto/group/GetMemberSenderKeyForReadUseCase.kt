package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg

class GetMemberSenderKeyForReadUseCase(
    private val getMemberSenderKey: GetMemberSenderKeyUseCase,
    private val local: CryptoKeyLocalDataSource,
    private val cryptoCore: CryptoCore,
    private val userKeyRepository: CryptoUserKeysLocalSource
) {
    suspend fun getMemberSenderKeyForRead(
        groupId: String,
        memberUserId: String,
        locationId: String,
        deviceKeyVersion: Long
    ): GroupMemberSenderKey {
        val currentKey = getMemberSenderKey.getMemberSenderKeyForReadWrite(
            groupId = groupId,
            memberUserId = memberUserId,
            locationId = locationId
        )

        val isCurrentKeyVersionValid = currentKey is GroupMemberSenderKey.ValidForRead && currentKey.deviceKeyVersion == deviceKeyVersion
        if (isCurrentKeyVersionValid) {
            return currentKey
        }

        return when (
            val archivedKey = getArchivedSenderKey(
                groupId = groupId,
                memberUserId = memberUserId,
                locationId = locationId,
                deviceKeyVersion = deviceKeyVersion
            )
        ) {
            is GroupMemberSenderKey.ValidForRead -> archivedKey
            else -> currentKey
        }
    }

    private suspend fun getArchivedSenderKey(
        groupId: String,
        memberUserId: String,
        locationId: String,
        deviceKeyVersion: Long
    ): GroupMemberSenderKey {
        val archivedKey = local.getArchivedGroupDeviceKey(
            groupId = groupId,
            ownerUserId = memberUserId,
            locationId = locationId
        )

        if (archivedKey == null || archivedKey.deviceKeyVersion != deviceKeyVersion) {
            return GroupMemberSenderKey.Error.MissingKeys
        }

        return try {
            GroupMemberSenderKey.ValidForRead(
                key = cryptoCore.decryptDataWithKey(
                    data = archivedKey.encryptedSenderKey,
                    key = userKeyRepository.personalKey(version = archivedKey.userKeyVersion).key
                ),
                deviceKeyVersion = archivedKey.deviceKeyVersion,
                memberKeyVersion = archivedKey.memberKeyVersion,
                isCurrent = false
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member")
                .d("Invalid archived device key for member $memberUserId in group $groupId (${e.message})")
            GroupMemberSenderKey.Error.Invalid
        }
    }
}
