package app.paralino.core.models.user

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class BlocklistModel(
    val id: String,
    val ownerUserId: String,
    val resourceId: String,
    val resourceType: BlocklistType,
    @Contextual
    val data: CryptoResource<BlockListModelData>?
)

@Serializable
data class BlockListModelData(
    val name: String,
    val image: ImageModel,
)

enum class BlocklistType {
    GROUP, USER, UNKNOWN;

    companion object {
        fun fromId(key: String): BlocklistType {
            return when(key) {
                "user" -> USER
                "group" -> GROUP
                else -> UNKNOWN
            }
        }
    }
}