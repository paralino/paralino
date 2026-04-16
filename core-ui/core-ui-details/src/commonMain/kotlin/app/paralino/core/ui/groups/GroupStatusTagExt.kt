package app.paralino.core.ui.groups

import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.common.GroupMemberItemState
import app.paralino.core.presentation.common.GroupStatusTagType
import app.paralino.core.utils.extensions.isLocationInaccurate

fun MemberDeviceStatusType.toStatusTagType(): GroupStatusTagType {
    return when (this) {
        MemberDeviceStatusType.GPS_OFF -> GroupStatusTagType.GPS_OFF
        MemberDeviceStatusType.MISSING_PERMISSIONS -> GroupStatusTagType.MISSING_PERMISSION
        MemberDeviceStatusType.NOT_SHARING -> GroupStatusTagType.NOT_SHARING
        MemberDeviceStatusType.BATTERY_SAVER_ON -> GroupStatusTagType.BATTERY_SAVING
        MemberDeviceStatusType.TOO_MANY_REQUESTS -> GroupStatusTagType.TOO_MANY_REQUESTS
        MemberDeviceStatusType.THROTTLING -> GroupStatusTagType.IDLE
        MemberDeviceStatusType.IDLE -> GroupStatusTagType.IDLE
        MemberDeviceStatusType.IDLE_NO_LOCATION -> GroupStatusTagType.IDLE_NO_LOCATION
        MemberDeviceStatusType.ACTIVE -> GroupStatusTagType.SHARING_ACTIVE
        MemberDeviceStatusType.ACTIVE_NO_LOCATION -> GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION
        MemberDeviceStatusType.INACTIVE -> GroupStatusTagType.INACTIVE
    }
}

fun GroupMemberItemState.toStatusTagType(expanded: Boolean): GroupStatusTagType? {
    return when {
        !this.memberState.isVerified -> GroupStatusTagType.UNVERIFIED
        !expanded || this.locations.isEmpty() || !this.allowShowingDevices -> {
            val tagType = this.statusTag?.toStatusTagType()
            val singleVisibleLocation = this.singleVisibleLocation
            if (singleVisibleLocation?.accuracyMeters != null && singleVisibleLocation.accuracyMeters.isLocationInaccurate()) {
                when (tagType) {
                    GroupStatusTagType.SHARING_ACTIVE,
                    GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION,
                    GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE -> {
                        GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE
                    }

                    GroupStatusTagType.IDLE,
                    GroupStatusTagType.IDLE_NOT_ACCURATE -> {
                        GroupStatusTagType.IDLE_NOT_ACCURATE
                    }

                    else -> tagType
                }
            } else {
                tagType
            }
        }

        else -> null
    }
}
