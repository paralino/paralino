package app.paralino.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.ButtonStyle
import kotlinx.coroutines.delay

@Composable
fun FilledButton(
    modifier: Modifier = Modifier,
    style: ButtonStyle,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    icon: Painter? = null,
    state: AppButtonState = AppButtonState.CONTENT,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    var buttonState by remember { mutableStateOf(AppButtonState.CONTENT) }
    val oldState = buttonState

    val newState = when (state) {
        AppButtonState.CHECK_THEN_CONTENT -> {
            when (oldState) {
                AppButtonState.LOADING -> {
                    AppButtonState.CHECK_THEN_CONTENT
                }

                else -> {
                    AppButtonState.CONTENT
                }
            }
        }

        else -> state
    }

    LaunchedEffect(onClick) {
        if (newState == AppButtonState.CHECK_THEN_CONTENT) {
            delay(700)
            buttonState = AppButtonState.CONTENT
        } else {
            buttonState = newState
        }
    }

    Button(
        modifier = style.defaultModifier.then(modifier),
        contentPadding = style.paddingValues,
        colors = colors,
        elevation = elevation,
        shape = style.shape,
        onClick = {
            if (buttonState == AppButtonState.CONTENT)
                onClick()
        },
        content = {
            val contentAlpha by animateFloatAsState(
                targetValue = if (newState == AppButtonState.CONTENT) 1f else 0f,
                animationSpec = tween(durationMillis = 300)
            )
            val loadingAlpha by animateFloatAsState(
                targetValue = if (newState == AppButtonState.LOADING) 1f else 0f,
                animationSpec = tween(durationMillis = 300)
            )
            val checkAlpha by animateFloatAsState(
                targetValue = if (newState == AppButtonState.CHECK_THEN_CONTENT) 1f else 0f,
                animationSpec = tween(durationMillis = 300)
            )

            Box(contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .defaultMinSize(minHeight = style.minContentHeight)
                ) {
                    if (icon != null) {
                        Icon(painter = icon, null, modifier = Modifier.size(style.iconSize))
                        Spacer(Modifier.size(style.iconSpacing))
                    }
                    content()
                }

                if (newState == AppButtonState.LOADING || loadingAlpha > 0f) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(style.minContentHeight)
                            .alpha(loadingAlpha)
                    )
                }

                if (newState == AppButtonState.CHECK_THEN_CONTENT || checkAlpha > 0f) {
                    Icon(
                        painter = CoreAppIcons.Check.asPainterResource(),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary,
                        modifier = Modifier
                            .size(style.minContentHeight)
                            .alpha(checkAlpha)
                    )
                }
            }
        }
    )
}

enum class AppButtonState {
    CONTENT, LOADING, CHECK_THEN_CONTENT;

    companion object {
        fun loadingState(isLoading: Boolean?): AppButtonState {
            return if (isLoading == null) CONTENT else if (isLoading) LOADING else CONTENT
        }

        fun loadingStateWithCheck(isLoading: Boolean?): AppButtonState {
            return if (isLoading == null) CONTENT else if (isLoading) LOADING else CHECK_THEN_CONTENT
        }
    }
}