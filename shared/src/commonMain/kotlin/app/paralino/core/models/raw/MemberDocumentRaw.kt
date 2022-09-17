package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupId: String,

    val ownerPublicKey: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val shouldRotateKeys: Boolean,

    val data: String,
    val mutableData: String? = null
) {

    fun toPostJson(): Map<String, Any?> {
        return buildMap {
            put("ownerUserId", ownerUserId)
            put("groupId", groupId)
            put("ownerPublicKey", ownerPublicKey)
            put("data", data)
            put("mutableData", mutableData)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): MemberDocumentRaw {
            return MemberDocumentRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as String,
                ownerPublicKey = map["ownerPublicKey"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                shouldRotateKeys = map["shouldRotateKeys"] as Boolean,
                data = map["data"] as String,
                mutableData = map["mutableData"] as? String,
            )
        }
    }
}
