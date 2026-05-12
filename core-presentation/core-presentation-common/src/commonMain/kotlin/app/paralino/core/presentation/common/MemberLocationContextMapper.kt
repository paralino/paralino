package app.paralino.core.presentation.common

import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.location.IGeofenceManager
import kotlin.math.roundToInt

class MemberLocationContextMapper(
    private val geofenceManager: IGeofenceManager,
    private val valueFormatter: IValueFormatter,
) {

    fun enrichOneLocationMemberItems(
        members: List<GroupMemberItemStateWithOneLocation>,
        places: List<PlaceListItemData>,
        myLocation: LocationDataModel?,
    ): List<GroupMemberItemStateWithOneLocation> {
        if (members.isEmpty()) return members
        val geofencePlaces = places.filter { it.lat != null && it.lng != null && it.radiusMeters > 0 }
        return members.map { item ->
            val loc = item.singleVisibleLocation ?: return@map item
            val enriched = enrichLocationState(loc, geofencePlaces, myLocation)
            if (enriched == loc) item else item.copy(singleLocation = enriched)
        }
    }

    fun enrichMemberLocationState(
        location: GroupMemberItemLocationState,
        places: List<PlaceListItemData>,
        myLocation: LocationDataModel?,
    ): List<GroupMemberItemLocationState> {
        return enrichMemberLocationStates(
            locations = listOf(location),
            places = places,
            myLocation = myLocation
        )
    }

    fun enrichMemberLocationStates(
        locations: List<GroupMemberItemLocationState>,
        places: List<PlaceListItemData>,
        myLocation: LocationDataModel?,
    ): List<GroupMemberItemLocationState> {
        if (locations.isEmpty()) return locations
        val geofencePlaces = places.filter { it.lat != null && it.lng != null && it.radiusMeters > 0 }
        return locations.map { enrichLocationState(it, geofencePlaces, myLocation) }
    }

    private fun enrichLocationState(
        loc: GroupMemberItemLocationState,
        geofencePlaces: List<PlaceListItemData>,
        myLocation: LocationDataModel?,
    ): GroupMemberItemLocationState {
        val atPlace = findTightestContainingPlaceName(
            lat = loc.lat,
            lng = loc.lng,
            places = geofencePlaces,
        )
        val distanceFromMeText = formatDistanceFromMe(
            myLocation = myLocation,
            memberLat = loc.lat,
            memberLng = loc.lng,
        )
        return if (atPlace == null && distanceFromMeText == null) {
            loc
        } else {
            loc.copy(
                atPlace = atPlace,
                distanceFromMeText = distanceFromMeText,
            )
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
        if (meters < 80) return null
        val coercedMeters = ((meters / 30).roundToInt() * 30).toDouble()
        return valueFormatter.getDistanceSmall(
            meters = coercedMeters,
            distanceType = DistanceType.DISTANCE,
        )
    }
}
