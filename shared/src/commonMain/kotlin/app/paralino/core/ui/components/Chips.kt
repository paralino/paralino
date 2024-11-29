package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBold


@Composable
fun SecurityNoticeInfoChip(
    modifier: Modifier = Modifier,
    icon: Painter = CoreAppIcons.Security.asPainterResource(),
    text: String,
    onClick: (() -> Unit)? = null
) {
    DataInfoChip(
        modifier = modifier,
        backgroundColor = AppColors.accentLight.copy(alpha = 0.3f),
        imageOrIcon = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AppColors.accent
            )
            Spacer(Modifier.size(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .requiredHeightIn(min = 40.dp)
                    .width(0.5.dp)
                    .background(color = AppColors.accent.copy(alpha = 0.2f))
            )
            Spacer(Modifier.size(8.dp))
        },
        text = {
            Text(
                text = text.annotatedBold(),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f)
            )
        },
        onClick = onClick
    )
}

@Composable
fun DataInfoChip(
    modifier: Modifier = Modifier,
    image: ImageModel?,
    text: String,
    backgroundColor: Color = MaterialTheme.colors.surface,
    hasEndArrow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    DataInfoChip(
        modifier = modifier,
        backgroundColor = backgroundColor,
        hasEndArrow = hasEndArrow,
        imageOrIcon = if (image != null) {
            {
                ImageModelImage(
                    imageModel = image,
                    modifier = Modifier
                        .size(26.dp)
                        .border(
                            0.5.dp,
                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                            RoundedCornerShape(6.dp)
                        ),
                    clipShape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
        } else null,
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle1,
                color = contentColorFor(backgroundColor)
            )
        },
        onClick = onClick
    )
}

@Composable
fun DataInfoChip(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    hasEndArrow: Boolean = false,
    imageOrIcon: (@Composable RowScope.() -> Unit)?,
    text: @Composable RowScope.() -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .let {
                if (onClick != null)
                    it.clickable { onClick() }
                else it
            }
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(vertical = 5.dp)
            .padding(start = 5.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (imageOrIcon != null) {
            imageOrIcon()
        } else {
            Spacer(modifier = Modifier.size(3.dp))
        }
        text()
        if (hasEndArrow) {
            Icon(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .width(20.dp),
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                contentDescription = null
            )
        }
    }
}

@Composable
fun SmallActionButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.error,
    contentColor: Color = MaterialTheme.colors.onError,
    shape: Shape = RoundedCornerShape(percent = 50),
    hasEndArrow: Boolean = false,
    icon: Painter?,
    text: @Composable () -> Unit,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .background(
                color = backgroundColor,
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.caption.copy(color = contentColor)) {
            text()
        }
        if (hasEndArrow) {
            Icon(
                modifier = Modifier
                    .width(20.dp),
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                tint = contentColor,
                contentDescription = null
            )
        } else {
            Spacer(modifier = Modifier.size(4.dp))
        }
    }
}