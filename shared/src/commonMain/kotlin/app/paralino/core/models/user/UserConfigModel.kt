package app.paralino.core.models.user

import app.paralino.core.db.main.user.UserConfigEntity

data class UserConfigModel(
    val nodes: List<UserConfigNode>
) {
    val billingCustomerId: String? get() = nodes.firstOrNull {
        it.type == "billingCustomerId"
    }?.data

    companion object {
        fun fromDatabase(nodes: List<UserConfigEntity>): UserConfigModel {
            return UserConfigModel(
                nodes = nodes.map { document ->
                    UserConfigNode(
                        id = document.id,
                        ownerUserId = document.ownerUserId,
                        type = document.type,
                        data = document.data
                    )
                }
            )
        }

        fun fromRemote(nodes: List<Map<String, Any>>): UserConfigModel {
            return UserConfigModel(
                nodes = nodes.map { document ->
                    UserConfigNode(
                        id = document["\$id"] as String,
                        ownerUserId = document["ownerUserId"] as String,
                        type = document["type"] as String,
                        data = (document["resourceData"] as? String?) ?: (document["data"] as? String?),
                    )
                }
            )
        }
    }
}

data class UserConfigNode(
    val id: String,
    val ownerUserId: String,
    val type: String,
    val data: String?
) {

}