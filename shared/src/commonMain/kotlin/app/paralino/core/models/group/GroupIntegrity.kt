package app.paralino.core.models.group

import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoHashProvider
import app.paralino.core.crypto.toData
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class GroupIntegrity(
    val timeUtc: String,
    val signature: String
) {

    fun isValidForGroup(signature: String): Boolean {
        return this.signature == signature
    }

    fun isValidForMember(signature: String): Boolean {
        val threshold = 30.toDuration(DurationUnit.DAYS)
        val timeValidity = timeUtc.isoWithOffsetToInstant()?.plus(threshold)?.let { it > nowUTC() } ?: false
        return this.signature == signature && timeValidity
    }

    companion object {
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
            return hashProvider.computeHash(seed, HashType.SHA256)
        }
    }
}
