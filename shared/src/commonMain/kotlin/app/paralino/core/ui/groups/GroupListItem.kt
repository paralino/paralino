package app.paralino.core.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage

@Composable
fun GroupListItem(
    data: GroupItemState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .run {
                if (data.isSelected) {
                    this.border(
                        width = 2.dp,
                        color = MaterialTheme.colors.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else this
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageModelImage(
            imageModel = data.image,
            modifier = Modifier
                .size(if (data.isLarge) 72.dp else 50.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(10.dp)
                ),
            clipShape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = data.name,
                style = MaterialTheme.typography.h6.copy(fontSize = if (data.isLarge) 20.sp else 16.sp),
                modifier = Modifier
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                    Icon(
                        painter = CoreAppIcons.Group.asPainterResource(),
                        modifier = Modifier.size(16.dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "${data.userCount}",
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                    )
                }
            }

            val status = when (data.uiType) {
                GroupItemState.GroupUiType.UNVERIFIED -> GroupStatusTagType.UNVERIFIED
                GroupItemState.GroupUiType.ACTIVE -> GroupStatusTagType.SHARING_ACTIVE
                GroupItemState.GroupUiType.INACTIVE -> null
            }
            GroupStatusTag(status)
        }

        if (data.canEdit) {
            Icon(
                painter = CoreAppIcons.Settings.asPainterResource(),
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onEdit() }
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)
                    .padding(10.dp)
            )
        }
    }
}