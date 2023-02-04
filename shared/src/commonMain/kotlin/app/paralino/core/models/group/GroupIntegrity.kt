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

    fun isValid(signature: String): Boolean {
        val signatureValidity = this.signature == signature
        val threshold = 7.toDuration(DurationUnit.DAYS)
        val timeValidity = timeUtc.isoWithOffsetToInstant()?.plus(threshold)?.let { it > nowUTC() } ?: false
        return signatureValidity && timeValidity
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
            return hashProvider.computeHash(seed.toData(hashProvider).content, HashType.SHA256).asBase64String(hashProvider)
        }
    }
}
