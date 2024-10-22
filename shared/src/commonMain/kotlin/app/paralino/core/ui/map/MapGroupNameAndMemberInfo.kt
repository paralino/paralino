package app.paralino.core.ui.map

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.theme.AppColors

@Composable
fun MapGroupNameAndMemberInfo(
    modifier: Modifier = Modifier,
    groupName: String,
    groupImage: ImageModel,
    userCount: Int,
    activeUserCount: Int,
    idleUserCount: Int,
    inactiveUserCount: Int,
    verified: Boolean,
    interactionVisibilityFraction: Float,
    onGroupClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(percent = 100)
            )
            .clip(RoundedCornerShape(percent = 100))
            .clickable(
                enabled = interactionVisibilityFraction == 0f,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onGroupClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageModelImage(
            imageModel = groupImage,
            useBackgroundForEmoji = false,
            clipShape = CircleShape,
            modifier = Modifier.size(56.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp)
                .padding(start = 16.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.subtitle1.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp,
                ),
                maxLines = 2,
                modifier = Modifier
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                    Icon(
                        painter = CoreAppIcons.Group.asPainterResource(),
                        modifier = Modifier.size(14.dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = "$userCount",
                        style = MaterialTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                    )
                }

                if (idleUserCount > 0 || inactiveUserCount > 0) {
                    Row(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(start = 4.dp, end = 6.dp)
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MapUserCountElement(
                            count = activeUserCount,
                            color = AppColors.accent
                        )

                        if (idleUserCount > 0) {
                            Spacer(Modifier.size(8.dp))
                            MapUserCountElement(
                                count = idleUserCount,
                                color = AppColors.highlightGold
                            )
                        }

                        if (inactiveUserCount > 0) {
                            Spacer(Modifier.size(8.dp))
                            MapUserCountElement(
                                count = inactiveUserCount,
                                color = AppColors.redMain
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .padding(4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = if (interactionVisibilityFraction == 1f) LocalIndication.current else null
                ) { onGroupClick() }
                .clip(RoundedCornerShape(percent = 100))
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled * interactionVisibilityFraction),
                    RoundedCornerShape(percent = 100)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (verified) {
                Icon(
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate((-90f) * (1f - interactionVisibilityFraction))
                )
            } else {
                Icon(
                    painter = CoreAppIcons.Warning.asPainterResource(),
                    tint = MaterialTheme.colors.onError,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.MapUserCountElement(
    count: Int,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = color,
                shape = CircleShape
            )
    )
    Spacer(Modifier.size(3.dp))
    Text(
        text = "$count",
        style = MaterialTheme.typography.caption.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
    )
}