package app.paralino.core.models.map

sealed interface MapHomeScreenFocusType {
    data object NONE : MapHomeScreenFocusType
    data class PLACE(val placeId: String) : MapHomeScreenFocusType
    data class PRIVACY_ZONE(val privacyZoneId: String) : MapHomeScreenFocusType
    data class MEMBER(
        val memberId: String,
        val memberUserId: String,
        val memberLocationId: String?,
        val memberKeyId: String,
        val isIdleActive: Boolean,
        val forceZoom: Boolean
    ) : MapHomeScreenFocusType

    data class LOCATION_HISTORY(
        val member: MEMBER?,
        val groupId: String,
        val memberUserId: String,
        val deviceId: String?
    ) : MapHomeScreenFocusType

    data object ALL_PLACES : MapHomeScreenFocusType
    data object ALL_PRIVACY_ZONES : MapHomeScreenFocusType
    data object ALL_MEMBERS : MapHomeScreenFocusType

    val areMembersFocused: Boolean get() = this == ALL_MEMBERS
    val arePlacesFocused: Boolean get() = this == ALL_PLACES
    val arePrivacyZonesFocused: Boolean get() = this == ALL_PRIVACY_ZONES
    val asLocationHistory: LOCATION_HISTORY? get() = this as? LOCATION_HISTORY
    val asMember: MEMBER? get() = this as? MEMBER
    val asPlace: PLACE? get() = this as? PLACE
    val asPrivacyZone: PRIVACY_ZONE? get() = this as? PRIVACY_ZONE
    val isLocationHistoryFocused: Boolean get() = this is LOCATION_HISTORY
    val isMemberOrPlaceOrZoneFocused: Boolean get() = this is MEMBER || this is PLACE || this is PRIVACY_ZONE
    val isMemberPlaceOrHistoryFocused: Boolean get() = isMemberOrPlaceOrZoneFocused || isLocationHistoryFocused
}