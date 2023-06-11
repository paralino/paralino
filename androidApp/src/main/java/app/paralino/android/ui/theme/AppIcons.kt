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
import app.paralino.core.ui.components.CoreAppIcons

object AppIcons {
    val Incognito @Composable get() = CoreAppIcons.Incognito
    val NotListedLocation @Composable get() = R.drawable.ic_not_listed_location.asPainterResource()
    val LibraryBooks @Composable get() = R.drawable.ic_library_books.asPainterResource()
    val PrivacyTip @Composable get() = R.drawable.ic_privacy_tip.asPainterResource()
    val Code @Composable get() = R.drawable.ic_code.asPainterResource()
    val AccountCircle @Composable get() = R.drawable.ic_account_circle.asPainterResource()
    val DeleteForever @Composable get() = R.drawable.ic_delete_forever.asPainterResource()
    val Password @Composable get() = R.drawable.ic_password.asPainterResource()
    val HowToReg @Composable get() = R.drawable.ic_how_to_reg.asPainterResource()
    val Lock @Composable get() = R.drawable.ic_lock.asPainterResource()
    val LockOpen @Composable get() = R.drawable.ic_lock_open.asPainterResource()
    val NotificationsOff @Composable get() = R.drawable.ic_notifications_off.asPainterResource()
    val Home @Composable get() = R.drawable.ic_home.asPainterResource()
    val Adjust @Composable get() = R.drawable.ic_adjust.asPainterResource()
    val CenterFocusStrong @Composable get() = R.drawable.ic_center_focus_strong.asPainterResource()
    val Groups @Composable get() = R.drawable.ic_groups.asPainterResource()
    val Person @Composable get() = R.drawable.ic_person.asPainterResource()
    val AutoFixHigh @Composable get() = R.drawable.ic_auto_fix_high.asPainterResource()
    val Logout @Composable get() = R.drawable.ic_logout.asPainterResource()
    val Share @Composable get() = R.drawable.ic_share.asPainterResource()
    val ContentCopy @Composable get() = R.drawable.ic_content_copy.asPainterResource()
    val Warning @Composable get() = R.drawable.ic_warning.asPainterResource()
    val LocationOn @Composable get() = R.drawable.ic_location_on.asPainterResource()
    val Group @Composable get() = R.drawable.ic_group.asPainterResource()
    val BatteryFull @Composable get() = R.drawable.ic_battery_full.asPainterResource()
    val PhoneAndroid @Composable get() = R.drawable.ic_phone_android.asPainterResource()
    val ExpandMore @Composable get() = R.drawable.ic_expand_more.asPainterResource()
    val ExpandLess @Composable get() = R.drawable.ic_expand_less.asPainterResource()
    val AccessTime @Composable get() = R.drawable.ic_schedule.asPainterResource()
    val Devices @Composable get() = R.drawable.ic_devices.asPainterResource()
    val Delete @Composable get() = R.drawable.ic_delete.asPainterResource()
    val PersonRemove @Composable get() = R.drawable.ic_person_remove.asPainterResource()
    val StackedLineChart @Composable get() = R.drawable.ic_stacked_line_chart.asPainterResource()
    val Speed @Composable get() = R.drawable.ic_speed.asPainterResource()
    val BatteryChargingFull @Composable get() = R.drawable.ic_battery_charging_full.asPainterResource()
    val Timer @Composable get() = R.drawable.ic_timer.asPainterResource()
    val MapsHomeWork @Composable get() = R.drawable.ic_home_work.asPainterResource()
    val Map @Composable get() = R.drawable.ic_map.asPainterResource()
    val ShareLocation @Composable get() = R.drawable.ic_share_location.asPainterResource()
    val StopCircle @Composable get() = R.drawable.ic_stop_circle.asPainterResource()
    val MoreVert @Composable get() = R.drawable.ic_more_vert.asPainterResource()
    val Edit @Composable get() = R.drawable.ic_edit.asPainterResource()
    val PersonAdd @Composable get() = R.drawable.ic_person_add.asPainterResource()
    val Security @Composable get() = R.drawable.ic_security.asPainterResource()
    val Save @Composable get() = R.drawable.ic_save.asPainterResource()
    val Close @Composable get() = R.drawable.ic_close.asPainterResource()

    val Email @Composable get() = CoreAppIcons.Email
    val NotificationsActive @Composable get() = CoreAppIcons.NotificationsActive
    val Add @Composable get() = R.drawable.ic_add.asPainterResource()
    val AddLink @Composable get() = R.drawable.ic_add_link.asPainterResource()
    val Key @Composable get() = R.drawable.ic_key.asPainterResource()
    val Block @Composable get() = R.drawable.ic_block.asPainterResource()
    val List @Composable get() = Icons.Rounded.FormatListBulleted

    @Composable
    fun Int.asImageVector(): ImageVector {
        return ImageVector.vectorResource(id = this)
    }

    @Composable
    fun Int.asPainterResource(): Painter {
        return painterResource(id = this)
    }
}