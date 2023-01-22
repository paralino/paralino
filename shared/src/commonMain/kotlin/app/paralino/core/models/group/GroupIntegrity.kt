package app.paralino.core.models.group

import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoHashProvider
import app.paralino.core.crypto.toData
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupWithMemberRaw
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class GroupIntegrity(
    val timeUtc: String,
    val signature: String
) {

    suspend fun isValid(groupIntegrity: GroupIntegrity): Boolean {
        val signatureValidity = groupIntegrity.signature == signature
        val threshold = 7.toDuration(DurationUnit.DAYS)
        val timeValidity = timeUtc.fromUTCtoInstant()?.plus(threshold)?.let { it > nowUTC() } ?: false
        return signatureValidity && timeValidity
    }

    companion object {
        fun createSignature(
            hashProvider: ICryptoHashProvider,
            group: GroupWithMemberRaw,
            groupKey: String,
            memberPublicKeys: List<GroupMemberCryptoKeyDataRaw>,
        ): String {
            return createSignature(
                hashProvider = hashProvider,
                memberVerifications = group.members.map { it.member.verification },
                groupKey = groupKey,
                encryptedMemberPublicKeys = memberPublicKeys.map { it.encryptedPublicKey }
            )
        }
        fun createSignature(
            hashProvider: ICryptoHashProvider,
            memberVerifications: List<String>,
            groupKey: String,
            encryptedMemberPublicKeys: List<String>,
        ): String {
            val ids = buildList {
                addAll(memberVerifications)
                add(groupKey)
                addAll(encryptedMemberPublicKeys)
            }
            val seed = ids.joinToString(separator = ",")
            return hashProvider.computeHash(seed.toData(hashProvider).content, HashType.SHA256).asBase64String(hashProvider)
        }
    }
}
