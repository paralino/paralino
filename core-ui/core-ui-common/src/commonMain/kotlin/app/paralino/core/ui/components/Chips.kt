package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional


@Composable
fun NoticeInfoChip(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.accentLight.copy(alpha = 0.2f),
    textColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f),
    text: String,
    icon: (@Composable RowScope.() -> Unit)?,
    onClick: (() -> Unit)? = null
) {
    DataInfoChip(
        modifier = modifier,
        backgroundColor = backgroundColor,
        imageOrIcon = {
            icon?.invoke(this)
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
                text = text.annotatedBoldCompose(textColor = textColor),
                style = MaterialTheme.typography.body2,
                color = textColor
            )
        },
        endSection = null,
        onClick = onClick
    )
}

@Composable
fun NoticeInfoChip(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.accentLight.copy(alpha = 0.2f),
    iconTint: Color = AppColors.accent,
    textColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f),
    icon: Painter = CoreAppIcons.Security.asPainterResource(),
    text: String,
    onClick: (() -> Unit)? = null
) {
    NoticeInfoChip(
        modifier = modifier,
        backgroundColor = backgroundColor,
        textColor = textColor,
        text = text,
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
        },
        onClick = onClick
    )
}

@Composable
fun DataInfoChip(
    modifier: Modifier = Modifier,
    innerPaddingValues: PaddingValues? = null,
    image: ImageModel?,
    text: String,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    shape: CornerBasedShape = RoundedCornerShape(10.dp),
    imageShape: CornerBasedShape = RoundedCornerShape(6.dp),
    hasEndArrow: Boolean = false,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    DataInfoChip(
        modifier = modifier,
        innerPaddingValues = innerPaddingValues,
        backgroundColor = backgroundColor,
        shape = shape,
        imageOrIcon = if (image != null) {
            {
                ImageModelImage(
                    imageModel = image,
                    modifier = Modifier
                        .size(26.dp)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                            shape = imageShape
                        ),
                    clipShape = imageShape
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
        } else {
            null
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle1.copy(lineHeight = 18.sp),
                color = contentColor
            )
        },
        endSection = if (isLoading) {
            {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            }
        } else if (hasEndArrow) {
            {
                Icon(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(20.dp),
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    tint = contentColor.copy(alpha = ContentAlpha.disabled),
                    contentDescription = null
                )
            }
        } else {
            null
        },
        onClick = onClick
    )
}

@Composable
fun DataInfoChip(
    modifier: Modifier = Modifier,
    innerPaddingValues: PaddingValues? = null,
    backgroundColor: Color = MaterialTheme.colors.surface,
    shape: CornerBasedShape = RoundedCornerShape(10.dp),
    imageOrIcon: (@Composable RowScope.() -> Unit)?,
    text: @Composable RowScope.() -> Unit,
    endSection: (@Composable RowScope.() -> Unit)?,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .let {
                if (onClick != null)
                    it.clickable { onClick() }
                else it
            }
            .clip(shape)
            .background(MaterialTheme.colors.background)
            .background(backgroundColor)
            .conditional(innerPaddingValues != null) {
                padding(innerPaddingValues ?: PaddingValues())
            }
            .conditional(innerPaddingValues == null) {
                padding(vertical = 5.dp)
                .padding(start = 5.dp, end = 8.dp)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (imageOrIcon != null) {
            imageOrIcon()
        } else {
            Spacer(modifier = Modifier.size(3.dp))
        }
        text()
        if (endSection != null) {
            endSection()
        }
    }
}

@Composable
fun SmallActionButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.error,
    contentColor: Color = MaterialTheme.colors.onError,
    shape: Shape = RoundedCornerShape(percent = 50),
    isLoading: Boolean = false,
    hasEndArrow: Boolean = false,
    compactMode: Boolean = false,
    icon: Painter?,
    iconSize: Dp = 14.dp,
    text: @Composable RowScope.() -> Unit,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = modifier
            .clip(shape)
            .conditional(onClick != null) {
                clickable(onClick = { onClick?.invoke()} )
            }
            .background(
                color = backgroundColor,
                shape = shape
            )
            .padding(horizontal = if (compactMode) 2.dp else 8.dp, vertical = if (compactMode) 2.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.caption.copy(color = contentColor)) {
            text()
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(4.dp),
                strokeWidth = 1.5.dp,
                color = contentColor
            )
        } else if (hasEndArrow) {
            Icon(
                modifier = Modifier
                    .width(20.dp),
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                tint = contentColor,
                contentDescription = null
            )
        } else {
            Spacer(modifier = Modifier.size(if (compactMode && icon != null) 0.dp else 4.dp))
        }
    }
}