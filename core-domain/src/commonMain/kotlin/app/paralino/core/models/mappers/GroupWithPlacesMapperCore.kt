package app.paralino.core.models.mappers

import app.paralino.core.models.group.UserGroupWithMembersAndPlaces
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupWithMembersAndPlacesRaw

class GroupWithPlacesMapperCore(
    private val groupMapperCore: GroupMapperCore,
    private val placesMapper: PlaceMapperCore,
    private val privacyZoneMapper: PrivacyZoneMapperCore,
) {
    suspend fun remote2CoreWithMembersAndPlaces(model: GroupWithMembersAndPlacesRaw): UserGroupWithMembersAndPlaces {
        val places = model.places.map {
            GroupPlaceWithSettings(
                place = placesMapper.remote2Core(
                    model = it.place,
                    groupOwnerId = model.group.group.ownerUserId
                ),
                settings = it.settings?.let { placesMapper.settingsMapper.remote2Core(it) }
            )
        }

        val privacyZones = model.privacyZones.map {
            privacyZoneMapper.remote2Core(it)
        }

        return UserGroupWithMembersAndPlaces(
            groupWithMembers = groupMapperCore.remote2CoreWithMembers(model.group),
            places = places,
            privacyZones = privacyZones,
        )
    }
}