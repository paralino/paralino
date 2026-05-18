package app.paralino.core.domain.location

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.ApproximateSharedLocationPayload
import app.paralino.core.models.location.LatLng
import app.paralino.core.repo.location.IGeofenceManager
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ApproximateLocationObfuscator(
    private val localSource: ApproximateSharedLocationLocalSource,
    private val geofenceManager: IGeofenceManager,
    private val cryptoCore: CryptoCore,
) {

    private val EARTH_RADIUS_METERS = 6_371_000.0

    suspend fun obfuscate(
        groupId: GroupOrSpaceId,
        trueLat: Double,
        trueLng: Double,
        radiusMeters: Int,
    ): LatLng {
        val cached = localSource.load(groupId = groupId)
        if (cached != null && cached.radiusMeters == radiusMeters) {
            logg.i(
                tag = "approximate_location",
                message = "Approximate location area exist for radius: $radiusMeters"
            )
            val distanceToShared = geofenceManager.distanceBetweenPoints(
                lat1 = trueLat,
                lng1 = trueLng,
                lat2 = cached.lat,
                lng2 = cached.lng,
            )

            if (distanceToShared <= radiusMeters) {
                logg.i(
                    tag = "approximate_location",
                    message = "Location matches current approximate location area"
                )
                return LatLng(lat = cached.lat, lng = cached.lng)
            }
        }

        logg.i(
            tag = "approximate_location",
            message = "Create and save new approximate location area with radius $radiusMeters"
        )

        val random = Random(
            seed = cryptoCore.randomId(length = 16, alphabet = AlphabetType.EXTENDED).hashCode()
        )
        val point = destinationPoint(
            centerLat = trueLat,
            centerLng = trueLng,
            radiusMeters = radiusMeters,
            random = random
        )

        localSource.save(
            groupId = groupId,
            payload = ApproximateSharedLocationPayload(
                lat = point.lat,
                lng = point.lng,
                radiusMeters = radiusMeters,
            )
        )

        return point
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
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS

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
