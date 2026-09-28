package app.paralino.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun IconButtonWithClickConfirmation(
    modifier: Modifier = Modifier,
    defaultIcon: DrawableResource,
    iconFactory: (@Composable DrawableResource.(() -> Unit) -> Unit) = {
        DefaultIconForIconButtonWithClickConfirmation(
            modifier = modifier,
            icon = this,
            onClick = it
        )
    },
    onClick: () -> Unit
) {
    var iconConfirmation by remember { mutableStateOf(false) }
    LaunchedEffect(iconConfirmation) {
        if (iconConfirmation) {
            delay(1.seconds)
            iconConfirmation = false
        }
    }

    AnimatedContent(
        targetState = if (iconConfirmation) {
            CoreAppIcons.Check
        } else {
            defaultIcon
        },
        content = { icon ->
            iconFactory(icon) {
                onClick()
                iconConfirmation = true
            }
        }
    )
}

@Composable
fun DefaultIconForIconButtonWithClickConfirmation(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    onClick: () -> Unit
) {
    Icon(
        modifier = modifier
            .size(32.dp)
            .clickable {
                onClick()
            }
            .padding(vertical = 4.dp),
        painter = icon.asPainterResource(),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground.copy(alpha = 0.8f)
    )
}