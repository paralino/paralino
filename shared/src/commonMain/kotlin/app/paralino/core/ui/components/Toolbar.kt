package app.paralino.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource

@Composable
fun DefaultToolbar(
    modifier: Modifier = Modifier,
    titleComposable: (@Composable () -> Unit)?,
    elevation: Dp = 4.dp,
    color: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        color = color,
        contentColor = contentColor,
        elevation = elevation,
        shape = RectangleShape,
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    if (navigationIcon == null) {
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(52.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompositionLocalProvider(
                                LocalContentAlpha provides ContentAlpha.high,
                                content = navigationIcon
                            )
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProvideTextStyle(value = MaterialTheme.typography.h6) {
                            CompositionLocalProvider(
                                LocalContentAlpha provides ContentAlpha.high,
                                content = {
                                    AnimatedVisibility(titleComposable != null) {
                                        ProvideTextStyle(
                                            value = MaterialTheme.typography.h6.copy(
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 22.sp
                                            )
                                        ) {
                                            titleComposable?.invoke()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Row(
                            Modifier.fillMaxHeight(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun DefaultToolbar(
    modifier: Modifier = Modifier,
    title: String?,
    elevation: Dp = 4.dp,
    color: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    DefaultToolbar(
        modifier = modifier,
        elevation = elevation,
        color = color,
        contentColor = contentColor,
        titleComposable = if (title != null) {
            @Composable {
                Text(
                    modifier = Modifier.padding(start = 12.dp),
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
        shape: Shape = RoundedCornerShape(12.dp),
        onClick: () -> Unit
    ) {
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(40.dp)
                .clickable { onClick() }
                .shadow(
                    elevation = elevation,
                    shape = shape
                )
                .clip(shape = shape)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = shape
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