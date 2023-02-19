package app.paralino.android.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import app.paralino.android.R

object AppIcons {
    val Incognito @Composable get() = R.drawable.ic_incognito.asImageVector()
    val NotListedLocation @Composable get() = R.drawable.ic_not_listed_location.asImageVector()
    val LibraryBooks @Composable get() = R.drawable.ic_library_books.asImageVector()
    val PrivacyTip @Composable get() = R.drawable.ic_privacy_tip.asImageVector()
    val Code @Composable get() = R.drawable.ic_code.asImageVector()
    val AccountCircle @Composable get() = R.drawable.ic_account_circle.asImageVector()
    val DeleteForever @Composable get() = R.drawable.ic_delete_forever.asImageVector()
    val Password @Composable get() = R.drawable.ic_password.asImageVector()
    val HowToReg @Composable get() = R.drawable.ic_how_to_reg.asImageVector()
    val Lock @Composable get() = R.drawable.ic_lock.asImageVector()
    val LockOpen @Composable get() = R.drawable.ic_lock_open.asImageVector()
    val NotificationsOff @Composable get() = R.drawable.ic_notifications_off.asImageVector()
    val Home @Composable get() = R.drawable.ic_home.asImageVector()
    val Adjust @Composable get() = R.drawable.ic_adjust.asImageVector()
    val CenterFocusStrong @Composable get() = R.drawable.ic_center_focus_strong.asImageVector()
    val Groups @Composable get() = R.drawable.ic_groups.asImageVector()
    val Person @Composable get() = R.drawable.ic_person.asImageVector()
    val Login @Composable get() = R.drawable.ic_login.asImageVector()
    val AutoFixHigh @Composable get() = R.drawable.ic_auto_fix_high.asImageVector()
    val Logout @Composable get() = R.drawable.ic_logout.asImageVector()
    val Share @Composable get() = R.drawable.ic_share.asImageVector()
    val ContentCopy @Composable get() = R.drawable.ic_content_copy.asImageVector()
    val Warning @Composable get() = R.drawable.ic_warning.asImageVector()
    val LocationOn @Composable get() = R.drawable.ic_location_on.asImageVector()
    val Group @Composable get() = R.drawable.ic_group.asImageVector()
    val BatteryFull @Composable get() = R.drawable.ic_battery_full.asImageVector()
    val PhoneAndroid @Composable get() = R.drawable.ic_phone_android.asImageVector()
    val ExpandMore @Composable get() = R.drawable.ic_expand_more.asImageVector()
    val ExpandLess @Composable get() = R.drawable.ic_expand_less.asImageVector()
    val AccessTime @Composable get() = R.drawable.ic_schedule.asImageVector()
    val Devices @Composable get() = R.drawable.ic_devices.asImageVector()
    val Delete @Composable get() = R.drawable.ic_delete.asImageVector()
    val PersonRemove @Composable get() = R.drawable.ic_person_remove.asImageVector()
    val StackedLineChart @Composable get() = R.drawable.ic_stacked_line_chart.asImageVector()
    val Speed @Composable get() = R.drawable.ic_speed.asImageVector()
    val BatteryChargingFull @Composable get() = R.drawable.ic_battery_charging_full.asImageVector()
    val Timer @Composable get() = R.drawable.ic_timer.asImageVector()
    val MapsHomeWork @Composable get() = R.drawable.ic_home_work.asImageVector()
    val Map @Composable get() = R.drawable.ic_map.asImageVector()
    val ShareLocation @Composable get() = R.drawable.ic_share_location.asImageVector()
    val StopCircle @Composable get() = R.drawable.ic_stop_circle.asImageVector()
    val MoreVert @Composable get() = R.drawable.ic_more_vert.asImageVector()
    val Edit @Composable get() = R.drawable.ic_edit.asImageVector()
    val PersonAdd @Composable get() = R.drawable.ic_person_add.asImageVector()
    val Security @Composable get() = R.drawable.ic_security.asImageVector()
    val Save @Composable get() = R.drawable.ic_save.asImageVector()
    val Close @Composable get() = R.drawable.ic_close.asImageVector()
    val VisibilityOff @Composable get() = R.drawable.ic_visibility_off.asImageVector()
    val Visibility @Composable get() = R.drawable.ic_visibility.asImageVector()
    val RadioButtonUnchecked get() = R.drawable.ic_radio_button_unchecked
    val CheckCircle get() = R.drawable.ic_check_circle
    val DragIndicator @Composable get() = R.drawable.ic_drag_indicator.asImageVector()
    val ChevronLeft @Composable get() = R.drawable.ic_chevron_left.asImageVector()
    val ChevronRight get() = R.drawable.ic_chevron_right
    val Email @Composable get() = R.drawable.ic_mail.asImageVector()
    val WifiOff @Composable get() = R.drawable.ic_wifi_off.asImageVector()
    val PhonelinkSetup @Composable get() = R.drawable.ic_phonelink_setup.asImageVector()
    val NotificationsActive @Composable get() = R.drawable.ic_notifications_active.asImageVector()
    val LocationOff @Composable get() = R.drawable.ic_location_off.asImageVector()
    val Check @Composable get() = R.drawable.ic_check.asImageVector()
    val Add @Composable get() = R.drawable.ic_add.asImageVector()
    val AddLink @Composable get() = R.drawable.ic_add_link.asImageVector()
    val Key @Composable get() = R.drawable.ic_key.asImageVector()
    
    @Composable
    fun Int.asImageVector(): ImageVector {
        return ImageVector.vectorResource(id = this)
    }

    @Composable
    fun Int.asPainterResource(): Painter {
        return painterResource(id = this)
    }
}