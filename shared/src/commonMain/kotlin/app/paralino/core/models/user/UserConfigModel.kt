package app.paralino.core.models.user

import app.paralino.core.db.main.user.UserConfigEntity

data class UserConfigModel(
    val billingCustomerId: String?,
    val mapTilerToken: String?
) {

    fun toDatabaseEntities(): List<UserConfigEntity> {
        return buildList {
            if (!this@UserConfigModel.billingCustomerId.isNullOrBlank()) {
                add(
                    UserConfigEntity(
                        id = "billingCustomerId",
                        type = "billingCustomerId",
                        data = this@UserConfigModel.billingCustomerId
                    )
                )
            }
        }
    }

    companion object {
        fun fromDatabase(
            nodes: List<UserConfigEntity>,
            mapTilerToken: String?
        ): UserConfigModel {
            return UserConfigModel(
                billingCustomerId = nodes.firstOrNull {
                    it.type == "billingCustomerId"
                }?.data,
                mapTilerToken = mapTilerToken
            )
        }

        fun fromRemote(data: Map<String, Any?>?): UserConfigModel {
            return UserConfigModel(
                billingCustomerId = data?.get("billingCustomerId") as? String?,
                mapTilerToken = data?.get("mapTilerToken") as? String?
            )
        }
    }
}