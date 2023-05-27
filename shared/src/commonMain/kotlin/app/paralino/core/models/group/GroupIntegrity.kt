package app.paralino.core.models.group

import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoHashProvider
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class GroupIntegrity(
    val timeUtc: String,
    val signature: String
) {

    private fun isTimeValid(duration: Duration): Boolean {
        return timeUtc.isoWithOffsetToInstant()?.plus(duration)?.let { it > nowUTC() } ?: false
    }

    fun isValidForGroup(signature: String): Boolean {
        return this.signature == signature
    }

    fun isValidForMember(signature: String): Boolean {
        val timeValidity = isTimeValid(30.toDuration(DurationUnit.DAYS))
        return this.signature == signature && timeValidity
    }

    fun isValidForDevice(signature: String): Boolean {
        val timeValidity = isTimeValid(7.toDuration(DurationUnit.DAYS))
        return this.signature == signature && timeValidity
    }

    companion object {
        fun createSignature(
            hashProvider: ICryptoHashProvider,
            memberVerifications: List<String>,
            encryptedMemberPublicKeys: List<String>,
        ): String {
            val ids = buildList {
                addAll(memberVerifications)
                addAll(encryptedMemberPublicKeys)
            }
            val seed = ids.joinToString(separator = ",")
            return hashProvider.computeHash(seed, HashType.SHA256)
        }
    }
}
