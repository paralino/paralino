package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun DataInfoChip(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    hasEndArrow: Boolean = false,
    image: ImageModel?,
    text: String,
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
        if (image != null) {
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
        } else {
            Spacer(modifier = Modifier.size(3.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            color = contentColorFor(backgroundColor)
        )
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