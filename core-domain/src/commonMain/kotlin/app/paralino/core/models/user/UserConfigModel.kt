package app.paralino.core.models.user

import app.paralino.core.db.main.user.UserConfigEntity
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

data class UserConfigModel(
    val lastUpdateTime: Instant,
    val billingCustomerId: String?,
    val mapTilerStyleToken: String?,
    val mapTilerSatelliteToken: String?
) {

    val isValid: Boolean get() = !lastUpdateTime.isOlderThan(5.days)

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

            add(
                UserConfigEntity(
                    id = "lastUpdateTime",
                    type = "lastUpdateTime",
                    data = this@UserConfigModel.lastUpdateTime.toString()
                )
            )
        }
    }

    companion object {
        fun fromDatabase(
            nodes: List<UserConfigEntity>,
            mapTilerStyleToken: String?,
            mapTilerSatelliteToken: String?
        ): UserConfigModel {
            return UserConfigModel(
                lastUpdateTime = nodes.firstOrNull {
                    it.type == "lastUpdateTime"
                }?.data?.isoWithOffsetToInstant() ?: Instant.DISTANT_PAST,
                billingCustomerId = nodes.firstOrNull {
                    it.type == "billingCustomerId"
                }?.data,
                mapTilerStyleToken = mapTilerStyleToken,
                mapTilerSatelliteToken = mapTilerSatelliteToken
            )
        }

        fun fromRemote(data: Map<String, Any?>?): UserConfigModel {
            return UserConfigModel(
                lastUpdateTime = nowUTC(),
                billingCustomerId = data?.get("billingCustomerId") as? String?,
                mapTilerStyleToken = data?.get("mapTilerStyleToken") as? String?,
                mapTilerSatelliteToken = data?.get("mapTilerSatelliteToken") as? String?
            )
        }
    }
}