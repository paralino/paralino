package app.paralino.core.domain.location

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.PrivacyZoneSharedLocationPayload
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.models.privacyzones.PrivacyZoneData
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.privacyzones.PrivacyZonesLocalDataSource
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class PrivacyZoneObfuscationResult(
    val lat: Double,
    val lng: Double,
    val uploadAccuracyMeters: Int,
    val shareStatus: Boolean,
    val message: String?,
)

class PrivacyZoneObfuscator(
    private val privacyZonesLocal: PrivacyZonesLocalDataSource,
    private val sharedLocationLocal: PrivacyZoneSharedLocationLocalSource,
    private val geofenceManager: IGeofenceManager,
    private val cryptoCore: CryptoCore,
) {

    private val earthRadiusMeters = 6_371_000.0

    suspend fun obfuscateIfInsideZone(
        groupId: GroupOrSpaceId,
        trueLat: Double,
        trueLng: Double,
    ): PrivacyZoneObfuscationResult? {
        val zones = privacyZonesLocal.getNonExpiredPrivacyZonesForGroupOnce(groupId = groupId.id)
        val matches = zones.mapNotNull { zone ->
            val data = zone.data.data ?: return@mapNotNull null
            if (data.isExpired()) return@mapNotNull null
            if (!isInsideZone(trueLat, trueLng, data)) {
                sharedLocationLocal.delete(groupId = groupId.id, privacyZoneId = zone.id)
                return@mapNotNull null
            }
            zone to data
        }

        val selected = matches.minWithOrNull(
            compareBy<Pair<GroupPrivacyZone, PrivacyZoneData>> { it.second.radiusMeters }
                .thenBy { it.second.expiresAt }
                .thenBy { it.first.id }
        ) ?: return null

        val (zone, data) = selected
        return obfuscateWithinZone(
            groupId = groupId.id,
            privacyZoneId = zone.id,
            centerLat = data.lat,
            centerLng = data.lng,
            radiusMeters = data.radiusMeters,
            trueLat = trueLat,
            trueLng = trueLng,
            shareStatus = data.notifyOthersInZone,
            message = data.inZoneMessage
        )
    }

    private suspend fun obfuscateWithinZone(
        groupId: String,
        privacyZoneId: String,
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Int,
        trueLat: Double,
        trueLng: Double,
        shareStatus: Boolean,
        message: String?,
    ): PrivacyZoneObfuscationResult {
        val cached = sharedLocationLocal.load(groupId = groupId, privacyZoneId = privacyZoneId)
        if (cached != null && cached.zoneRadiusMeters == radiusMeters) {
            val distanceToShared = geofenceManager.distanceBetweenPoints(
                lat1 = trueLat,
                lng1 = trueLng,
                lat2 = cached.lat,
                lng2 = cached.lng,
            )
            if (distanceToShared <= radiusMeters) {
                logg.i("privacy_zone_location", "Reusing privacy zone shared location for $privacyZoneId")
                return PrivacyZoneObfuscationResult(
                    lat = cached.lat,
                    lng = cached.lng,
                    uploadAccuracyMeters = cached.uploadAccuracyMeters,
                    shareStatus = shareStatus,
                    message = message
                )
            }
        }

        logg.i("privacy_zone_location", "Creating new privacy zone shared location for $privacyZoneId")
        val random = Random(
            seed = cryptoCore.randomId(length = 16, alphabet = AlphabetType.EXTENDED).hashCode()
        )
        val point = destinationPoint(
            centerLat = centerLat,
            centerLng = centerLng,
            radiusMeters = radiusMeters,
            random = random,
        )

        val uploadAccuracyMeters = computeUploadAccuracyMeters(
            zoneRadiusMeters = radiusMeters,
            centerLat = centerLat,
            centerLng = centerLng,
            pickedLat = point.lat,
            pickedLng = point.lng,
        )

        sharedLocationLocal.save(
            groupId = groupId,
            privacyZoneId = privacyZoneId,
            payload = PrivacyZoneSharedLocationPayload(
                lat = point.lat,
                lng = point.lng,
                zoneRadiusMeters = radiusMeters,
                uploadAccuracyMeters = uploadAccuracyMeters,
            ),
        )

        return PrivacyZoneObfuscationResult(
            lat = point.lat,
            lng = point.lng,
            uploadAccuracyMeters = uploadAccuracyMeters,
            shareStatus = shareStatus,
            message = message
        )
    }

    private fun computeUploadAccuracyMeters(
        zoneRadiusMeters: Int,
        centerLat: Double,
        centerLng: Double,
        pickedLat: Double,
        pickedLng: Double,
    ): Int {
        val distanceFromCenter = geofenceManager.distanceBetweenPoints(
            lat1 = centerLat,
            lng1 = centerLng,
            lat2 = pickedLat,
            lng2 = pickedLng,
        ).toInt()
        return zoneRadiusMeters + distanceFromCenter
    }

    private fun isInsideZone(trueLat: Double, trueLng: Double, data: PrivacyZoneData): Boolean {
        return geofenceManager.distanceBetweenPoints(
            lat1 = trueLat,
            lng1 = trueLng,
            lat2 = data.lat,
            lng2 = data.lng,
        ) <= data.radiusMeters
    }

    private fun destinationPoint(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Int,
        random: Random,
    ): LatLng {
        val bearingRadians = random.nextDouble(0.0, 2 * kotlin.math.PI)
        val distanceMeters = sqrt(random.nextDouble()) * radiusMeters
        val latRad = centerLat.toRadians()
        val lngRad = centerLng.toRadians()
        val angularDistance = distanceMeters / earthRadiusMeters

        val newLatRad = asin(
            sin(latRad) * cos(angularDistance) +
                cos(latRad) * sin(angularDistance) * cos(bearingRadians),
        )
        val newLngRad = lngRad + atan2(
            sin(bearingRadians) * sin(angularDistance) * cos(latRad),
            cos(angularDistance) - sin(latRad) * sin(newLatRad),
        )

        return LatLng(lat = newLatRad.toDegrees(), lng = newLngRad.toDegrees())
    }

    private fun Double.toRadians() = this * kotlin.math.PI / 180.0
    private fun Double.toDegrees() = this * 180.0 / kotlin.math.PI
}
