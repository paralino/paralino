package app.paralino.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource

@Composable
fun DefaultToolbar(
    modifier: Modifier = Modifier,
    titleComposable: (@Composable () -> Unit)?,
    elevation: Dp = 4.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.background,
        elevation = elevation,
        title = {
            AnimatedVisibility (titleComposable != null) {
                ProvideTextStyle(value = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)) {
                    titleComposable?.invoke()
                }
            }
        },
        navigationIcon = navigationIcon,
        actions = actions
    )
}

@Composable
fun DefaultToolbar(
    modifier: Modifier = Modifier,
    title: String?,
    elevation: Dp = 4.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    DefaultToolbar(
        modifier = modifier,
        elevation = elevation,
        titleComposable = if (title != null) {
            @Composable {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            null
        },
        navigationIcon = navigationIcon,
        actions = actions
    )
}

object ToolbarButtons {
    @Composable
    fun Back(
        elevation: Dp = 0.dp,
        onClick: () -> Unit
    ) {
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(40.dp)
                .clickable { onClick() }
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(shape = RoundedCornerShape(12.dp))
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp),
            painter = CoreAppIcons.ChevronLeft.asPainterResource(),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface
        )
    }

    @Composable
    fun Close(onClick: () -> Unit) {
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(40.dp)
                .clickable { onClick() }
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp),
            painter = CoreAppIcons.Close.asPainterResource(),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface
        )
    }
}