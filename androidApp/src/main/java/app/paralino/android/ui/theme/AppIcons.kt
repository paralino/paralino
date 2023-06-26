package app.paralino.android.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import app.paralino.android.R
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource

object AppIcons {
    val NotListedLocation @Composable get() = R.drawable.ic_not_listed_location.asPainterResource()
    val NotificationsOff @Composable get() = R.drawable.ic_notifications_off.asPainterResource()
    val Home @Composable get() = R.drawable.ic_home.asPainterResource()
    val Adjust @Composable get() = R.drawable.ic_adjust.asPainterResource()
    val CenterFocusStrong @Composable get() = R.drawable.ic_center_focus_strong.asPainterResource()
    val Groups @Composable get() = R.drawable.ic_groups.asPainterResource()
    val Logout @Composable get() = CoreAppIcons.Logout.asPainterResource()
    val Warning @Composable get() = CoreAppIcons.Warning.asPainterResource()
    val Group @Composable get() = CoreAppIcons.Group.asPainterResource()
    val ExpandMore @Composable get() = CoreAppIcons.ExpandMore.asPainterResource()
    val StackedLineChart @Composable get() = CoreAppIcons.StackedLineChart.asPainterResource()
    val Timer @Composable get() = CoreAppIcons.Timer.asPainterResource()
    val Speed @Composable get() = CoreAppIcons.Speed.asPainterResource()
    val AccessTime @Composable get() = CoreAppIcons.AccessTime.asPainterResource()
    val Delete @Composable get() = CoreAppIcons.Delete.asPainterResource()
    val BatteryChargingFull @Composable get() = R.drawable.ic_battery_charging_full.asPainterResource()
    val MapsHomeWork @Composable get() = R.drawable.ic_home_work.asPainterResource()
    val Map @Composable get() = R.drawable.ic_map.asPainterResource()
    val ShareLocation @Composable get() = R.drawable.ic_share_location.asPainterResource()
    val StopCircle @Composable get() = R.drawable.ic_stop_circle.asPainterResource()
    val MoreVert @Composable get() = R.drawable.ic_more_vert.asPainterResource()
    val Edit @Composable get() = CoreAppIcons.Edit.asPainterResource()
    val PersonAdd @Composable get() = R.drawable.ic_person_add.asPainterResource()
    val Security @Composable get() = CoreAppIcons.Security.asPainterResource()
    val Save @Composable get() = CoreAppIcons.Save.asPainterResource()
    val Close @Composable get() = CoreAppIcons.Close.asPainterResource()

    val Add @Composable get() = CoreAppIcons.Add.asPainterResource()
    val Block @Composable get() = CoreAppIcons.Block.asPainterResource()
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