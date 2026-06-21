package app.paralino.core.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.paralino.core.resources.icons.Res
import app.paralino.core.resources.icons.ic_account_circle
import app.paralino.core.resources.icons.ic_add
import app.paralino.core.resources.icons.ic_add_link
import app.paralino.core.resources.icons.ic_admin_shield_person
import app.paralino.core.resources.icons.ic_admin_shield_person_v2
import app.paralino.core.resources.icons.ic_admin_shield_star
import app.paralino.core.resources.icons.ic_arrow_open_in_new
import app.paralino.core.resources.icons.ic_arrow_spinny
import app.paralino.core.resources.icons.ic_assignment_list
import app.paralino.core.resources.icons.ic_auto_fix_high
import app.paralino.core.resources.icons.ic_bar_chart_stats
import app.paralino.core.resources.icons.ic_battery_charging_full
import app.paralino.core.resources.icons.ic_battery_full
import app.paralino.core.resources.icons.ic_block
import app.paralino.core.resources.icons.ic_chat_bubble_list
import app.paralino.core.resources.icons.ic_check
import app.paralino.core.resources.icons.ic_check_circle
import app.paralino.core.resources.icons.ic_chevron_left
import app.paralino.core.resources.icons.ic_chevron_right
import app.paralino.core.resources.icons.ic_circle_circle
import app.paralino.core.resources.icons.ic_circle_cluster_dots
import app.paralino.core.resources.icons.ic_circle_only_outline
import app.paralino.core.resources.icons.ic_circle_with_dot
import app.paralino.core.resources.icons.ic_circle_with_outline
import app.paralino.core.resources.icons.ic_close
import app.paralino.core.resources.icons.ic_code
import app.paralino.core.resources.icons.ic_communication
import app.paralino.core.resources.icons.ic_content_copy
import app.paralino.core.resources.icons.ic_delete
import app.paralino.core.resources.icons.ic_delete_forever
import app.paralino.core.resources.icons.ic_devices
import app.paralino.core.resources.icons.ic_directions
import app.paralino.core.resources.icons.ic_download_arrow
import app.paralino.core.resources.icons.ic_drag_indicator
import app.paralino.core.resources.icons.ic_edit
import app.paralino.core.resources.icons.ic_elevation_2
import app.paralino.core.resources.icons.ic_expand_less
import app.paralino.core.resources.icons.ic_expand_more
import app.paralino.core.resources.icons.ic_family_star
import app.paralino.core.resources.icons.ic_filter_tune
import app.paralino.core.resources.icons.ic_focus_person_off
import app.paralino.core.resources.icons.ic_focus_person_on
import app.paralino.core.resources.icons.ic_group
import app.paralino.core.resources.icons.ic_groups
import app.paralino.core.resources.icons.ic_hand_swipe_touch
import app.paralino.core.resources.icons.ic_home
import app.paralino.core.resources.icons.ic_home_work
import app.paralino.core.resources.icons.ic_how_to_reg
import app.paralino.core.resources.icons.ic_incognito
import app.paralino.core.resources.icons.ic_info_circle
import app.paralino.core.resources.icons.ic_key
import app.paralino.core.resources.icons.ic_launcher_foreground
import app.paralino.core.resources.icons.ic_library_books
import app.paralino.core.resources.icons.ic_license_tag
import app.paralino.core.resources.icons.ic_license_ticket
import app.paralino.core.resources.icons.ic_location_off
import app.paralino.core.resources.icons.ic_location_on
import app.paralino.core.resources.icons.ic_location_on_filled
import app.paralino.core.resources.icons.ic_lock
import app.paralino.core.resources.icons.ic_lock_open
import app.paralino.core.resources.icons.ic_login
import app.paralino.core.resources.icons.ic_logout
import app.paralino.core.resources.icons.ic_mail
import app.paralino.core.resources.icons.ic_map
import app.paralino.core.resources.icons.ic_more_vert
import app.paralino.core.resources.icons.ic_not_listed_location
import app.paralino.core.resources.icons.ic_not_listed_location_filled
import app.paralino.core.resources.icons.ic_notifications_active
import app.paralino.core.resources.icons.ic_notifications_off
import app.paralino.core.resources.icons.ic_open_book
import app.paralino.core.resources.icons.ic_password
import app.paralino.core.resources.icons.ic_person
import app.paralino.core.resources.icons.ic_person_add
import app.paralino.core.resources.icons.ic_person_remove
import app.paralino.core.resources.icons.ic_phone_android
import app.paralino.core.resources.icons.ic_phonelink_setup
import app.paralino.core.resources.icons.ic_polyline
import app.paralino.core.resources.icons.ic_privacy_tip
import app.paralino.core.resources.icons.ic_privacy_zone
import app.paralino.core.resources.icons.ic_question_mark_circle
import app.paralino.core.resources.icons.ic_radio_button_unchecked
import app.paralino.core.resources.icons.ic_refresh
import app.paralino.core.resources.icons.ic_replay
import app.paralino.core.resources.icons.ic_route_dots
import app.paralino.core.resources.icons.ic_ruler
import app.paralino.core.resources.icons.ic_save
import app.paralino.core.resources.icons.ic_schedule
import app.paralino.core.resources.icons.ic_search
import app.paralino.core.resources.icons.ic_security
import app.paralino.core.resources.icons.ic_settings
import app.paralino.core.resources.icons.ic_share
import app.paralino.core.resources.icons.ic_share_location
import app.paralino.core.resources.icons.ic_shield_lock
import app.paralino.core.resources.icons.ic_signal_cell_1
import app.paralino.core.resources.icons.ic_signal_cell_2
import app.paralino.core.resources.icons.ic_signal_cell_3
import app.paralino.core.resources.icons.ic_speed
import app.paralino.core.resources.icons.ic_stacked_line_chart
import app.paralino.core.resources.icons.ic_star
import app.paralino.core.resources.icons.ic_star_circle_filled
import app.paralino.core.resources.icons.ic_star_shine
import app.paralino.core.resources.icons.ic_stop_circle
import app.paralino.core.resources.icons.ic_sync
import app.paralino.core.resources.icons.ic_target_circles
import app.paralino.core.resources.icons.ic_timeline
import app.paralino.core.resources.icons.ic_timeline_path
import app.paralino.core.resources.icons.ic_timer
import app.paralino.core.resources.icons.ic_visibility
import app.paralino.core.resources.icons.ic_visibility_off
import app.paralino.core.resources.icons.ic_warning
import app.paralino.core.resources.icons.ic_wifi_1
import app.paralino.core.resources.icons.ic_wifi_2
import app.paralino.core.resources.icons.ic_wifi_3
import app.paralino.core.resources.icons.ic_wifi_off
import app.paralino.core.resources.icons.ic_zoom_focus_full
import app.paralino.core.resources.icons.ic_zoom_focus_large
import app.paralino.core.resources.icons.ic_zoom_focus_small
import app.paralino.core.resources.icons.ic_zoom_in_map
import app.paralino.core.resources.icons.ic_zoom_out_map
import app.paralino.core.resources.icons.image_map_mockup_dark
import app.paralino.core.resources.icons.image_map_mockup_light
import app.paralino.core.resources.icons.image_map_satellite_lawn
import app.paralino.core.resources.icons.paralino_start_background
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

object CoreAppIcons {
    val LauncherLogo get() = Res.drawable.ic_launcher_foreground
    val Check get() = Res.drawable.ic_check
    val PhonelinkSetup get() = Res.drawable.ic_phonelink_setup
    val NotificationsActive get() = Res.drawable.ic_notifications_active
    val LocationOff get() = Res.drawable.ic_location_off
    val Email get() = Res.drawable.ic_mail
    val WifiOff get() = Res.drawable.ic_wifi_off
    val Incognito get() = Res.drawable.ic_incognito
    val Person get() = Res.drawable.ic_person
    val VisibilityOff get() = Res.drawable.ic_visibility_off
    val Visibility get() = Res.drawable.ic_visibility
    val RadioButtonUnchecked get() = Res.drawable.ic_radio_button_unchecked
    val CheckCircle get() = Res.drawable.ic_check_circle
    val DragIndicator get() = Res.drawable.ic_drag_indicator
    val ChevronLeft get() = Res.drawable.ic_chevron_left
    val ChevronRight get() = Res.drawable.ic_chevron_right
    val Warning get() = Res.drawable.ic_warning
    val LocationOn get() = Res.drawable.ic_location_on
    val LocationOnFilled get() = Res.drawable.ic_location_on_filled
    val Group get() = Res.drawable.ic_group
    val Close get() = Res.drawable.ic_close
    val Add get() = Res.drawable.ic_add
    val Key get() = Res.drawable.ic_key
    val Save get() = Res.drawable.ic_save
    val ContentCopy get() = Res.drawable.ic_content_copy
    val Delete get() = Res.drawable.ic_delete
    val Share get() = Res.drawable.ic_share
    val AutoFixHigh get() = Res.drawable.ic_auto_fix_high
    val Logout get() = Res.drawable.ic_logout
    val Login get() = Res.drawable.ic_login
    val PrivacyTip get() = Res.drawable.ic_privacy_tip
    val Code get() = Res.drawable.ic_code
    val AccountCircle get() = Res.drawable.ic_account_circle
    val DeleteForever get() = Res.drawable.ic_delete_forever
    val Password get() = Res.drawable.ic_password
    val LibraryBooks = Res.drawable.ic_library_books
    val HowToReg get() = Res.drawable.ic_how_to_reg
    val Edit get() = Res.drawable.ic_edit
    val Security get() = Res.drawable.ic_security
    val AddLink get() = Res.drawable.ic_add_link
    val Block get() = Res.drawable.ic_block
    val Lock get() = Res.drawable.ic_lock
    val LockOpen get() = Res.drawable.ic_lock_open
    val BatteryFull get() = Res.drawable.ic_battery_full
    val PhoneAndroid get() = Res.drawable.ic_phone_android
    val ExpandMore get() = Res.drawable.ic_expand_more
    val ExpandLess get() = Res.drawable.ic_expand_less
    val Devices get() = Res.drawable.ic_devices
    val PersonRemove get() = Res.drawable.ic_person_remove
    val StackedLineChart get() = Res.drawable.ic_stacked_line_chart
    val Timer get() = Res.drawable.ic_timer
    val Speed get() = Res.drawable.ic_speed
    val AccessTime get() = Res.drawable.ic_schedule
    val BatteryChargingFull get() = Res.drawable.ic_battery_charging_full
    val MapsHomeWork get() = Res.drawable.ic_home_work
    val Map get() = Res.drawable.ic_map
    val Home get() = Res.drawable.ic_home
    val ShareLocation get() = Res.drawable.ic_share_location
    val StopCircle get() = Res.drawable.ic_stop_circle
    val MoreVert get() = Res.drawable.ic_more_vert
    val PersonAdd get() = Res.drawable.ic_person_add
    val Settings get() = Res.drawable.ic_settings
    val Ruler get() = Res.drawable.ic_ruler
    val Communication get() = Res.drawable.ic_communication
    val FilterTune get() = Res.drawable.ic_filter_tune
    val QuestionCircle get() = Res.drawable.ic_question_mark_circle
    val InfoCircle get() = Res.drawable.ic_info_circle
    val OpenBook get() = Res.drawable.ic_open_book
    val ChatBubbleList get() = Res.drawable.ic_chat_bubble_list
    val AssignmentList get() = Res.drawable.ic_assignment_list
    val Search get() = Res.drawable.ic_search
    val DownloadArrow get() = Res.drawable.ic_download_arrow
    val AdminShieldStar get() = Res.drawable.ic_admin_shield_star
    val AdminShieldPerson get() = Res.drawable.ic_admin_shield_person
    val AdminShieldPersonV2 get() = Res.drawable.ic_admin_shield_person_v2
    val ShieldLock get() = Res.drawable.ic_shield_lock

    val Refresh get() = Res.drawable.ic_refresh
    val Replay get() = Res.drawable.ic_replay
    val Sync get() = Res.drawable.ic_sync
    val ArrowOpenNew get() = Res.drawable.ic_arrow_open_in_new
    val ArrowSpinny get() = Res.drawable.ic_arrow_spinny
    val StarFamily get() = Res.drawable.ic_family_star
    val Star get() = Res.drawable.ic_star
    val StarCircle get() = Res.drawable.ic_star_circle_filled
    val StarShine get() = Res.drawable.ic_star_shine
    val LicenseTag get() = Res.drawable.ic_license_tag
    val LicenseTicket get() = Res.drawable.ic_license_ticket

    val NotListedLocation get() = Res.drawable.ic_not_listed_location
    val NotListedLocationFilled get() = Res.drawable.ic_not_listed_location_filled
    val PrivacyZone get() = Res.drawable.ic_privacy_zone
    val NotificationsOff get() = Res.drawable.ic_notifications_off
    val CircleWithDot get() = Res.drawable.ic_circle_with_dot
    val Groups get() = Res.drawable.ic_groups

    val Elevation get() = Res.drawable.ic_elevation_2
    val PolylineWithDots get() = Res.drawable.ic_timeline
    val TimelinePathWithPins get() = Res.drawable.ic_timeline_path
    val Directions get() = Res.drawable.ic_directions
    val BarChartStats get() = Res.drawable.ic_bar_chart_stats
    val CircleCluster get() = Res.drawable.ic_circle_cluster_dots
    val PolylineWithCircles get() = Res.drawable.ic_polyline
    val TargetCircles get() = Res.drawable.ic_target_circles
    val CircleCircle get() = Res.drawable.ic_circle_circle
    val RouteLineWithDotsOnEnds get() = Res.drawable.ic_route_dots

    val FocusPersonOn get() = Res.drawable.ic_focus_person_on
    val FocusPersonOff get() = Res.drawable.ic_focus_person_off
    val ZoomInFocusMap get() = Res.drawable.ic_zoom_in_map
    val ZoomOutFocusMap get() = Res.drawable.ic_zoom_out_map
    val ZoomFocusSmall get() = Res.drawable.ic_zoom_focus_small
    val ZoomFocusLarge get() = Res.drawable.ic_zoom_focus_large
    val ZoomFocusFull get() = Res.drawable.ic_zoom_focus_full
    val HandSwipeTouch get() = Res.drawable.ic_hand_swipe_touch

    val SignalWifi3 get() = Res.drawable.ic_wifi_3
    val SignalWifi2 get() = Res.drawable.ic_wifi_2
    val SignalWifi1 get() = Res.drawable.ic_wifi_1
    val SignalCell3 get() = Res.drawable.ic_signal_cell_3
    val SignalCell2 get() = Res.drawable.ic_signal_cell_2
    val SignalCell1 get() = Res.drawable.ic_signal_cell_1

//    val CircleFull get() = MR.images.ic_circle_full
    val CircleWithOutline get() = Res.drawable.ic_circle_with_outline
    val CircleOnlyOutline get() = Res.drawable.ic_circle_only_outline
//    val MapModeSatellite get() = Res.drawable.image_map_type_satellite
//    val MapModeDefaultLight get() = Res.drawable.image_map_type_default_light
//    val MapModeDefaultDark get() = Res.drawable.image_map_type_default_dark

    val MapMockupDark get() = Res.drawable.image_map_mockup_dark
    val MapMockupLight get() = Res.drawable.image_map_mockup_light
    val MapMockupSatelliteLawn get() = Res.drawable.image_map_satellite_lawn

    val StartBackground get() = Res.drawable.paralino_start_background

    @Composable
    inline fun DrawableResource.asPainterResource(): Painter {
        return painterResource(resource = this)
    }
}