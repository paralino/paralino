package app.paralino.core.presentation.common

import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.current
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
        ConditionErrorType.UnrestrictedBackgroundAccess -> when (DeviceType.current) {
            DeviceType.Android -> MR.strings.location_banner_error_background_access
            DeviceType.iOS -> MR.strings.location_banner_error_background_app_refresh
        }
        ConditionErrorType.PushNotificationService.PushNotificationServiceOff -> MR.strings.location_banner_error_push_service_off
        ConditionErrorType.PushNotificationService.PushNotificationServiceNoConstantConnection -> MR.strings.location_banner_error_push_service_no_constant_connection
    }

val ConditionErrorType.resolvedNameRes: StringResource?
    get() = when (this) {
        ConditionErrorType.GPS -> MR.strings.location_banner_resolved_gps_on
        ConditionErrorType.PermissionNotifications -> MR.strings.location_banner_resolved_permission_notifications
        is ConditionErrorType.PermissionLocation -> MR.strings.location_banner_resolved_permission_location
        ConditionErrorType.PermissionLocationBackground -> MR.strings.location_banner_resolved_permission_location_background
        ConditionErrorType.UnrestrictedBackgroundAccess -> when (DeviceType.current) {
            DeviceType.Android -> MR.strings.location_banner_resolved_background_access
            DeviceType.iOS -> MR.strings.location_banner_resolved_background_app_refresh
        }
        is ConditionErrorType.PushNotificationService -> MR.strings.location_banner_resolved_push_service
        ConditionErrorType.Network -> MR.strings.location_banner_resolved_online
        ConditionErrorType.NewAppUpdate -> MR.strings.location_banner_resolved_app_up_to_date
        is ConditionErrorType.EmailVerification,
        ConditionErrorType.GuestAccount -> null
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
        is ConditionErrorType.PushNotificationService -> MR.strings.location_banner_fix
    }