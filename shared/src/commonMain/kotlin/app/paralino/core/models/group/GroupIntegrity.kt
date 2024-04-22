package app.paralino.core.models.group

import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoHashProvider
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Serializable
data class GroupIntegrity(
    val timeUtc: String,
    val signature: String
) {

    private fun isTimeValid(duration: Duration): Boolean {
        return timeUtc.isoWithOffsetToInstant()?.plus(duration)?.let { it > nowUTC() && it < nowUTC().plus(7.days) } ?: false
    }

    fun isValidForGroup(signature: String): Boolean {
        val timeValidity = isTimeValid(7.days)
        return this.signature == signature && timeValidity
    }

    fun isValidForMember(signature: String): Boolean {
        val timeValidity = isTimeValid(2.hours)
        return this.signature == signature && timeValidity
    }

    fun isValidForDevice(signature: String): Boolean {
        val timeValidity = isTimeValid(60.minutes)
        return this.signature == signature && timeValidity
    }

    companion object {
        fun createSignature(
            hashProvider: ICryptoHashProvider,
            serverIntegrity: String,
            memberVerifications: List<String>,
            encryptedMemberPublicKeys: List<String>,
        ): String {
            val ids = buildList {
                add(serverIntegrity)
                addAll(memberVerifications)
                addAll(encryptedMemberPublicKeys)
            }
            val seed = ids.joinToString(separator = ",")
            return hashProvider.computeHash(seed, HashType.SHA256)
        }
    }
}
