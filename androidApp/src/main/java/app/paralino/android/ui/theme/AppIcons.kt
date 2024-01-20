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
    val NotificationsOff @Composable get() = CoreAppIcons.NotificationsOff.asPainterResource()
    val Adjust @Composable get() = CoreAppIcons.Adjust.asPainterResource()
    val Home @Composable get() = R.drawable.ic_home.asPainterResource()
    val CenterFocusStrong @Composable get() = R.drawable.ic_center_focus_strong.asPainterResource()
    val Warning @Composable get() = CoreAppIcons.Warning.asPainterResource()
    val Group @Composable get() = CoreAppIcons.Group.asPainterResource()
    val ExpandMore @Composable get() = CoreAppIcons.ExpandMore.asPainterResource()
    val MapsHomeWork @Composable get() = CoreAppIcons.MapsHomeWork.asPainterResource()
    val MoreVert @Composable get() = CoreAppIcons.MoreVert.asPainterResource()
    val Edit @Composable get() = CoreAppIcons.Edit.asPainterResource()
    val Security @Composable get() = CoreAppIcons.Security.asPainterResource()
    val Save @Composable get() = CoreAppIcons.Save.asPainterResource()
    val Close @Composable get() = CoreAppIcons.Close.asPainterResource()

    val Add @Composable get() = CoreAppIcons.Add.asPainterResource()
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