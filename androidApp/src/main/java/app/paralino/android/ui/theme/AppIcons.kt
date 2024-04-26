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
    val Adjust @Composable get() = CoreAppIcons.Adjust.asPainterResource()
    val Warning @Composable get() = CoreAppIcons.Warning.asPainterResource()
    val Group @Composable get() = CoreAppIcons.Group.asPainterResource()
    val ExpandMore @Composable get() = CoreAppIcons.ExpandMore.asPainterResource()
    val Save @Composable get() = CoreAppIcons.Save.asPainterResource()
    val Close @Composable get() = CoreAppIcons.Close.asPainterResource()

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