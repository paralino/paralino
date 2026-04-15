package app.paralino.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.conditional
import kotlinx.coroutines.delay

@Composable
fun FilledButton(
    modifier: Modifier = Modifier,
    style: ButtonStyle,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    elevation: Dp = if (enabled) 4.dp else 0.dp,
    icon: Painter? = null,
    state: AppButtonState = AppButtonState.CONTENT,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)?,
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

    LaunchedEffect(newState) {
        if (newState == AppButtonState.CHECK_THEN_CONTENT) {
            delay(700)
            buttonState = AppButtonState.CONTENT
        } else {
            buttonState = newState
        }
    }

    Box(
        modifier = style.defaultModifier.then(modifier)
            .clickable(onClick = {
                if (buttonState == AppButtonState.CONTENT) {
                    onClick()
                }
            })
            .conditional(elevation > 0.dp) {
                dropShadow(
                    shape = style.shape,
                    shadow = Shadow(
                        radius = elevation,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                    )
                )
            }
            .clip(style.shape)
            .background(color = colors.backgroundColor(enabled = enabled).value)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(style.paddingValues),
        contentAlignment = Alignment.Center
    ) {
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

        CompositionLocalProvider(LocalContentColor provides colors.contentColor(enabled = enabled).value) {
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

                if (content != null) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.button) {
                        content()
                    }
                }

                if (icon != null && content != null) {
                    Spacer(Modifier.size(4.dp))
                }
            }

            if (newState == AppButtonState.LOADING || loadingAlpha > 0f) {
                CircularProgressIndicator(
                    color = colors.contentColor(enabled = enabled).value,
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
                    tint = colors.contentColor(enabled = enabled).value,
                    modifier = Modifier
                        .size(style.minContentHeight)
                        .alpha(checkAlpha)
                )
            }
        }
    }
}

@Composable
fun FullWidthActionButton(
    modifier: Modifier = Modifier,
    startIcon: Painter,
    text: String,
    endIcon: Painter? = CoreAppIcons.ChevronRight.asPainterResource(),
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = startIcon,
            modifier = Modifier.size(18.dp),
            tint = contentColor,
            contentDescription = null
        )

        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(weight = 1f)
        )

        if (endIcon != null) {
            Icon(
                modifier = Modifier.width(20.dp),
                painter = endIcon,
                tint = contentColor.copy(alpha = ContentAlpha.disabled),
                contentDescription = null
            )
        } else {
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
fun CheckButtonIcon(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    size: Dp = 24.dp,
    backgroundCheckedColor: Color = if (isSystemInDarkTheme()) {
        MaterialTheme.colors.onPrimary
    } else {
        MaterialTheme.colors.primary
    },
    contentCheckedColor: Color = if (isSystemInDarkTheme()) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onPrimary
    },
    backgroundUncheckedColor: Color = MaterialTheme.colors.surface,
    contentUncheckedColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
    checkedIcon: Painter = CoreAppIcons.CheckCircle.asPainterResource(),
    uncheckedIcon: Painter = CoreAppIcons.RadioButtonUnchecked.asPainterResource(),
) {
    if (isChecked) {
        Icon(
            modifier = modifier
                .size(size)
                .background(
                    color = backgroundCheckedColor,
                    shape = CircleShape
                )
                .padding(1.dp),
            painter = checkedIcon,
            contentDescription = null,
            tint = contentCheckedColor
        )
    } else {
        Icon(
            modifier = modifier
                .size(size)
                .background(color = backgroundUncheckedColor, shape = CircleShape)
                .padding(2.dp),
            painter = uncheckedIcon,
            contentDescription = null,
            tint = contentUncheckedColor
        )
    }
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