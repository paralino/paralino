package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.presentation.settings.SettingsActionItemType
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.utils.extensions.annotatedBoldCompose

@Composable
fun SettingsActionItem(
    modifier: Modifier = Modifier,
    name: String,
    icon: Painter,
    tag: String? = null,
    description: String? = null,
    nameTextStyle: TextStyle = MaterialTheme.typography.subtitle2.copy(fontSize = 18.sp),
    iconBackgroundTint: Color = MaterialTheme.colors.surface,
    iconTint: Color = MaterialTheme.colors.onSurface,
    type: SettingsActionItemType = SettingsActionItemType.Navigation,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(enabled = onClick != null || onLongClick != null, onLongClick = onLongClick, onClick = { onClick?.invoke() })
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                tint = iconTint,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = iconBackgroundTint,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    modifier = Modifier,
                    text = name,
                    style = nameTextStyle,
                    textAlign = TextAlign.Start
                )
                if (tag != null) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier,
                            text = tag.annotatedBoldCompose(),
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            when (type) {
                SettingsActionItemType.Action -> {}
                SettingsActionItemType.Navigation -> {
                    Icon(
                        modifier = Modifier.width(20.dp),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                        contentDescription = null
                    )
                }
                SettingsActionItemType.Link -> {
                    Icon(
                        modifier = Modifier.width(20.dp),
                        painter = CoreAppIcons.ArrowOpenNew.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                        contentDescription = null
                    )
                }

                is SettingsActionItemType.Switch -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                    ) {
                        if (type.checked) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isSystemInDarkTheme()) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary,
                                        shape = CircleShape
                                    )
                                    .padding(1.dp),
                                painter = CoreAppIcons.CheckCircle.asPainterResource(),
                                contentDescription = null,
                                tint = if (isSystemInDarkTheme()) MaterialTheme.colors.primary else MaterialTheme.colors.onPrimary,
                            )
                        } else {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                painter = CoreAppIcons.RadioButtonUnchecked.asPainterResource(),
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.disabled)
                            )
                        }
                    }
                }

                SettingsActionItemType.Loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onBackground,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(22.dp)
                    )
                }
            }
        }
        if (description != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .padding(horizontal = 4.dp),
                    text = description.annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
