package app.paralino.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.paralino.core.MR
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import paralino.shared.generated.resources.Res
import paralino.shared.generated.resources.ic_account_circle
import paralino.shared.generated.resources.ic_add
import paralino.shared.generated.resources.ic_add_link
import paralino.shared.generated.resources.ic_arrow_open_in_new
import paralino.shared.generated.resources.ic_arrow_spinny
import paralino.shared.generated.resources.ic_assignment_list
import paralino.shared.generated.resources.ic_auto_fix_high
import paralino.shared.generated.resources.ic_bar_chart_stats
import paralino.shared.generated.resources.ic_battery_charging_full
import paralino.shared.generated.resources.ic_battery_full
import paralino.shared.generated.resources.ic_block
import paralino.shared.generated.resources.ic_chat_bubble_list
import paralino.shared.generated.resources.ic_check
import paralino.shared.generated.resources.ic_check_circle
import paralino.shared.generated.resources.ic_chevron_left
import paralino.shared.generated.resources.ic_chevron_right
import paralino.shared.generated.resources.ic_circle_circle
import paralino.shared.generated.resources.ic_circle_cluster_dots
import paralino.shared.generated.resources.ic_circle_only_outline
import paralino.shared.generated.resources.ic_circle_with_dot
import paralino.shared.generated.resources.ic_circle_with_outline
import paralino.shared.generated.resources.ic_close
import paralino.shared.generated.resources.ic_code
import paralino.shared.generated.resources.ic_communication
import paralino.shared.generated.resources.ic_content_copy
import paralino.shared.generated.resources.ic_delete
import paralino.shared.generated.resources.ic_delete_forever
import paralino.shared.generated.resources.ic_devices
import paralino.shared.generated.resources.ic_directions
import paralino.shared.generated.resources.ic_drag_indicator
import paralino.shared.generated.resources.ic_edit
import paralino.shared.generated.resources.ic_elevation_2
import paralino.shared.generated.resources.ic_expand_less
import paralino.shared.generated.resources.ic_expand_more
import paralino.shared.generated.resources.ic_family_star
import paralino.shared.generated.resources.ic_filter_tune
import paralino.shared.generated.resources.ic_focus_person_off
import paralino.shared.generated.resources.ic_focus_person_on
import paralino.shared.generated.resources.ic_group
import paralino.shared.generated.resources.ic_groups
import paralino.shared.generated.resources.ic_home
import paralino.shared.generated.resources.ic_home_work
import paralino.shared.generated.resources.ic_how_to_reg
import paralino.shared.generated.resources.ic_incognito
import paralino.shared.generated.resources.ic_info_circle
import paralino.shared.generated.resources.ic_key
import paralino.shared.generated.resources.ic_launcher_foreground
import paralino.shared.generated.resources.ic_library_books
import paralino.shared.generated.resources.ic_location_off
import paralino.shared.generated.resources.ic_location_on
import paralino.shared.generated.resources.ic_location_on_filled
import paralino.shared.generated.resources.ic_lock
import paralino.shared.generated.resources.ic_lock_open
import paralino.shared.generated.resources.ic_login
import paralino.shared.generated.resources.ic_logout
import paralino.shared.generated.resources.ic_mail
import paralino.shared.generated.resources.ic_map
import paralino.shared.generated.resources.ic_more_vert
import paralino.shared.generated.resources.ic_not_listed_location
import paralino.shared.generated.resources.ic_not_listed_location_filled
import paralino.shared.generated.resources.ic_notifications_active
import paralino.shared.generated.resources.ic_notifications_off
import paralino.shared.generated.resources.ic_open_book
import paralino.shared.generated.resources.ic_password
import paralino.shared.generated.resources.ic_person
import paralino.shared.generated.resources.ic_person_add
import paralino.shared.generated.resources.ic_person_remove
import paralino.shared.generated.resources.ic_phone_android
import paralino.shared.generated.resources.ic_phonelink_setup
import paralino.shared.generated.resources.ic_polyline
import paralino.shared.generated.resources.ic_privacy_tip
import paralino.shared.generated.resources.ic_question_mark_circle
import paralino.shared.generated.resources.ic_radio_button_unchecked
import paralino.shared.generated.resources.ic_refresh
import paralino.shared.generated.resources.ic_replay
import paralino.shared.generated.resources.ic_route_dots
import paralino.shared.generated.resources.ic_ruler
import paralino.shared.generated.resources.ic_save
import paralino.shared.generated.resources.ic_schedule
import paralino.shared.generated.resources.ic_search
import paralino.shared.generated.resources.ic_security
import paralino.shared.generated.resources.ic_settings
import paralino.shared.generated.resources.ic_share
import paralino.shared.generated.resources.ic_share_location
import paralino.shared.generated.resources.ic_signal_cell_1
import paralino.shared.generated.resources.ic_signal_cell_2
import paralino.shared.generated.resources.ic_signal_cell_3
import paralino.shared.generated.resources.ic_speed
import paralino.shared.generated.resources.ic_stacked_line_chart
import paralino.shared.generated.resources.ic_star
import paralino.shared.generated.resources.ic_star_circle_filled
import paralino.shared.generated.resources.ic_star_shine
import paralino.shared.generated.resources.ic_stop_circle
import paralino.shared.generated.resources.ic_sync
import paralino.shared.generated.resources.ic_target_circles
import paralino.shared.generated.resources.ic_timeline
import paralino.shared.generated.resources.ic_timeline_path
import paralino.shared.generated.resources.ic_timer
import paralino.shared.generated.resources.ic_visibility
import paralino.shared.generated.resources.ic_visibility_off
import paralino.shared.generated.resources.ic_warning
import paralino.shared.generated.resources.ic_wifi_1
import paralino.shared.generated.resources.ic_wifi_2
import paralino.shared.generated.resources.ic_wifi_3
import paralino.shared.generated.resources.ic_wifi_off
import paralino.shared.generated.resources.ic_zoom_focus_full
import paralino.shared.generated.resources.ic_zoom_focus_large
import paralino.shared.generated.resources.ic_zoom_focus_small
import paralino.shared.generated.resources.ic_zoom_in_map
import paralino.shared.generated.resources.ic_zoom_out_map
import paralino.shared.generated.resources.image_map_mockup_dark
import paralino.shared.generated.resources.image_map_mockup_light
import paralino.shared.generated.resources.image_map_satellite_lawn
import paralino.shared.generated.resources.paralino_start_background

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

    val Refresh get() = Res.drawable.ic_refresh
    val Replay get() = Res.drawable.ic_replay
    val Sync get() = Res.drawable.ic_sync
    val ArrowOpenNew get() = Res.drawable.ic_arrow_open_in_new
    val ArrowSpinny get() = Res.drawable.ic_arrow_spinny
    val StarFamily get() = Res.drawable.ic_family_star
    val Star get() = Res.drawable.ic_star
    val StarCircle get() = Res.drawable.ic_star_circle_filled
    val StarShine get() = Res.drawable.ic_star_shine

    val NotListedLocation get() = Res.drawable.ic_not_listed_location
    val NotListedLocationFilled get() = Res.drawable.ic_not_listed_location_filled
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

    val SignalWifi3 get() = Res.drawable.ic_wifi_3
    val SignalWifi2 get() = Res.drawable.ic_wifi_2
    val SignalWifi1 get() = Res.drawable.ic_wifi_1
    val SignalCell3 get() = Res.drawable.ic_signal_cell_3
    val SignalCell2 get() = Res.drawable.ic_signal_cell_2
    val SignalCell1 get() = Res.drawable.ic_signal_cell_1

    val CircleFull get() = MR.images.ic_circle_full
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

    @Composable
    fun ImageResource.asPainterResource(): Painter {
        return dev.icerock.moko.resources.compose.painterResource(this)
    }
}