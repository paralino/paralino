package app.paralino.core.presentation.map

import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.presentation.common.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.location.IGeofenceManager
import kotlin.math.roundToInt

class MapMemberLocationContextMapper(
    private val geofenceManager: IGeofenceManager,
    private val valueFormatter: IValueFormatter,
) {

    fun enrich(
        members: List<GroupMemberItemStateWithOneLocation>,
        places: List<PlaceListItemData>,
        myLocation: LocationDataModel?,
    ): List<GroupMemberItemStateWithOneLocation> {
        if (members.isEmpty()) return members
        val geofencePlaces =
            places.filter { it.lat != null && it.lng != null && it.radiusMeters > 0 }
        return members.map { item ->
            val loc = item.singleVisibleLocation ?: return@map item
            val atPlace = findTightestContainingPlaceName(
                lat = loc.lat,
                lng = loc.lng,
                places = geofencePlaces
            )
            val distanceFromMe = formatDistanceFromMe(
                myLocation = myLocation,
                memberLat = loc.lat,
                memberLng = loc.lng
            )
            if (atPlace == null && distanceFromMe == null) {
                item
            } else {
                item.copy(
                    singleLocation = loc.copy(
                        atPlace = atPlace,
                        distanceFromMeText = distanceFromMe,
                    )
                )
            }
        }
    }

    private fun findTightestContainingPlaceName(
        lat: Double?,
        lng: Double?,
        places: List<PlaceListItemData>,
    ): String? {
        if (lat == null || lng == null) return null
        val inside = places.filter { place ->
            val plat = place.lat ?: return@filter false
            val plng = place.lng ?: return@filter false
            val dist = geofenceManager.distanceBetweenPoints(lat, lng, plat, plng)
            dist <= place.radiusMeters
        }
        return inside.minByOrNull { it.radiusMeters }?.name
    }

    private fun formatDistanceFromMe(
        myLocation: LocationDataModel?,
        memberLat: Double?,
        memberLng: Double?,
    ): String? {
        if (myLocation == null || memberLat == null || memberLng == null) return null
        val meters = geofenceManager.distanceBetweenPoints(
            lat1 = myLocation.lat,
            lng1 = myLocation.lng,
            lat2 = memberLat,
            lng2 = memberLng,
        )
        if (meters < 50) return null
        val coercedMeters = ((meters / 20).roundToInt() * 20).toDouble()
        return valueFormatter.getDistanceSmall(
            meters = coercedMeters,
            distanceType = DistanceType.DISTANCE
        )
    }
}
