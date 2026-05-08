package app.paralino.core.presentation.common

import app.paralino.core.MR
import app.paralino.core.utils.ConditionErrorType
import dev.icerock.moko.resources.StringResource

val ConditionErrorType.nameRes: StringResource
    get() = when (this) {
        ConditionErrorType.GPS -> MR.strings.location_banner_error_gps_off
        ConditionErrorType.PermissionNotifications -> MR.strings.location_banner_error_no_permission_notifications
        ConditionErrorType.PermissionLocation.PermissionLocationAny -> MR.strings.location_banner_error_no_permission_location
        ConditionErrorType.PermissionLocation.PermissionLocationPrecise -> MR.strings.location_banner_error_no_permission_location_precise
        ConditionErrorType.PermissionLocationBackground -> MR.strings.location_banner_error_no_permission_location_background
        ConditionErrorType.Network -> MR.strings.location_banner_error_offline
        is ConditionErrorType.EmailVerification -> MR.strings.info_email_confirm
        ConditionErrorType.GuestAccount -> MR.strings.settings_account_guest_badge
        ConditionErrorType.NewAppUpdate -> MR.strings.app_version_alert_title
        ConditionErrorType.UnrestrictedBackgroundAccess -> MR.strings.location_banner_error_background_access
    }

val ConditionErrorType.endActionTextRes: StringResource?
    get() = when (this) {
        ConditionErrorType.GPS -> MR.strings.location_banner_fix
        ConditionErrorType.PermissionNotifications -> MR.strings.location_banner_fix
        is ConditionErrorType.PermissionLocation -> MR.strings.location_banner_fix
        ConditionErrorType.PermissionLocationBackground -> MR.strings.location_banner_fix
        ConditionErrorType.Network -> null
        is ConditionErrorType.EmailVerification -> MR.strings.button_verify
        ConditionErrorType.GuestAccount -> MR.strings.location_banner_fix
        ConditionErrorType.NewAppUpdate -> MR.strings.app_version_alert_button_update
        ConditionErrorType.UnrestrictedBackgroundAccess -> MR.strings.location_banner_fix
    }