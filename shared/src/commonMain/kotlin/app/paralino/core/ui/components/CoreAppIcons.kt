package app.paralino.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.paralino.core.MR
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
object CoreAppIcons {
    val List get() = "drawable/ic_login.xml"
    val Check get() = "drawable/ic_check.xml"
    val PhonelinkSetup get() = "drawable/ic_phonelink_setup.xml"
    val NotificationsActive get() = "drawable/ic_notifications_active.xml"
    val LocationOff get() = "drawable/ic_location_off.xml"
    val Email get() = "drawable/ic_mail.xml"
    val WifiOff get() = "drawable/ic_wifi_off.xml"
    val Incognito get() = "drawable/ic_incognito.xml"
    val Person get() = "drawable/ic_person.xml"
    val VisibilityOff get() = "drawable/ic_visibility_off.xml"
    val Visibility get() = "drawable/ic_visibility.xml"
    val RadioButtonUnchecked get() = "drawable/ic_radio_button_unchecked.xml"
    val CheckCircle get() = "drawable/ic_check_circle.xml"
    val DragIndicator get() = "drawable/ic_drag_indicator.xml"
    val ChevronLeft get() = "drawable/ic_chevron_left.xml"
    val ChevronRight get() = "drawable/ic_chevron_right.xml"
    val Warning get() = "drawable/ic_warning.xml"
    val LocationOn get() = "drawable/ic_location_on.xml"
    val Group get() = "drawable/ic_group.xml"
    val Close get() = "drawable/ic_close.xml"
    val Add get() = "drawable/ic_add.xml"
    val Key get() = "drawable/ic_key.xml"
    val Save get() = "drawable/ic_save.xml"
    val ContentCopy get() = "drawable/ic_content_copy.xml"
    val Delete get() = "drawable/ic_delete.xml"
    val Share get() = "drawable/ic_share.xml"
    val AutoFixHigh get() = "drawable/ic_auto_fix_high.xml"
    val Logout get() = "drawable/ic_logout.xml"
    val PrivacyTip get() = "drawable/ic_privacy_tip.xml"
    val Code get() = "drawable/ic_code.xml"
    val AccountCircle get() = "drawable/ic_account_circle.xml"
    val DeleteForever get() = "drawable/ic_delete_forever.xml"
    val Password get() = "drawable/ic_password.xml"
    val LibraryBooks = "drawable/ic_library_books.xml"
    val HowToReg get() = "drawable/ic_how_to_reg.xml"
    val Edit get() = "drawable/ic_edit.xml"
    val Security get() = "drawable/ic_security.xml"
    val AddLink get() = "drawable/ic_add_link.xml"
    val Block get() = "drawable/ic_block.xml"
    val Lock get() = "drawable/ic_lock.xml"
    val LockOpen get() = "drawable/ic_lock_open.xml"
    val BatteryFull get() = "drawable/ic_battery_full.xml"
    val PhoneAndroid get() = "drawable/ic_phone_android.xml"
    val ExpandMore get() = "drawable/ic_expand_more.xml"
    val ExpandLess get() = "drawable/ic_expand_less.xml"
    val Devices get() = "drawable/ic_devices.xml"
    val PersonRemove get() = "drawable/ic_person_remove.xml"
    val StackedLineChart get() = "drawable/ic_stacked_line_chart.xml"
    val Timer get() = "drawable/ic_timer.xml"
    val Speed get() = "drawable/ic_speed.xml"
    val AccessTime get() = "drawable/ic_schedule.xml"
    val BatteryChargingFull get() = "drawable/ic_battery_charging_full.xml"
    val MapsHomeWork get() = "drawable/ic_home_work.xml"
    val Map get() = "drawable/ic_map.xml"
    val Home get() = "drawable/ic_home.xml"
    val ShareLocation get() = "drawable/ic_share_location.xml"
    val StopCircle get() = "drawable/ic_stop_circle.xml"
    val MoreVert get() = "drawable/ic_more_vert.xml"
    val PersonAdd get() = "drawable/ic_person_add.xml"

    val NotListedLocation get() = "drawable/ic_not_listed_location.xml"
    val NotificationsOff get() = "drawable/ic_notifications_off.xml"
    val Adjust get() = "drawable/ic_adjust.xml"
    val Groups get() = "drawable/ic_groups.xml"

    val SignalWifi3 get() = "drawable/ic_wifi_3.xml"
    val SignalWifi2 get() = "drawable/ic_wifi_2.xml"
    val SignalWifi1 get() = "drawable/ic_wifi_1.xml"
    val SignalCell3 get() = "drawable/ic_signal_cell_3.xml"
    val SignalCell2 get() = "drawable/ic_signal_cell_2.xml"
    val SignalCell1 get() = "drawable/ic_signal_cell_1.xml"

    val CircleFull get() = MR.images.ic_circle_full
    val CircleWithOutline get() = "drawable/ic_circle_with_outline.xml"
    val MapModeSatellite get() = "drawable-nodpi/image_map_type_satellite.jpg"
    val MapModeDefaultLight get() = "drawable-nodpi/image_map_type_default_light.png"
    val MapModeDefaultDark get() = "drawable-nodpi/image_map_type_default_dark.png"

    @Composable
    fun String.asPainterResource(): Painter {
        return painterResource(resource = DrawableResource(path = this))
    }

    @Composable
    fun ImageResource.asPainterResource(): Painter {
        return dev.icerock.moko.resources.compose.painterResource(this)
    }
}