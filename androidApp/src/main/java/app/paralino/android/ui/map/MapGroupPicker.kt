package app.paralino.android.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.paralino.android.R
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.utils.extensions.quantityStringResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold

@Composable
fun MapGroupPicker(
    modifier: Modifier = Modifier,
    groupName: String,
    groupImage: ImageModel,
    userCount: Int,
    inactiveUserCount: Int,
    verified: Boolean,
    onGroupClick: () -> Unit,
    onExpandClick: () -> Unit,
) {
    val height: Dp = remember { 56.dp }
    ConstraintLayout(
        modifier = modifier
            .height(height)

    ) {

        val (image, center, expand, background) = createRefs()
        val backgroundColor = MaterialTheme.colors.background

        Canvas(
            modifier = Modifier
                .constrainAs(background) {
                    linkTo(
                        start = image.start,
                        end = expand.end,
                        top = parent.top,
                        bottom = parent.bottom,
                    )
                    this.width = Dimension.fillToConstraints
                    this.height = Dimension.fillToConstraints
                }
                .clip(RoundedCornerShape(height))
                .clickable { onGroupClick() },
            onDraw = {
                val pos = this.size.minDimension
                val factor = 1.1f
                val offset = (pos * factor - pos) / 2
                val circlePath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(
                                Offset(x = 0f - offset, y = 0f - offset),
                                Size(width = pos * 2 * factor, height = pos * factor)
                            ),
                            cornerRadius = CornerRadius(pos * factor, pos * factor)
                        )
                    )
                }
                clipPath(circlePath, clipOp = ClipOp.Difference) {
                    drawRoundRect(backgroundColor, cornerRadius = CornerRadius(pos, pos))
                }
            }
        )

        // image
        Row(
            modifier = Modifier
                .height(height)
                .constrainAs(image) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = CircleShape
                )
                .clip(RoundedCornerShape(height))
                .clickable { onExpandClick() }
                .padding(horizontal = 8.dp)
        ) {
            ImageModelImage(
                imageModel = groupImage,
                useBackgroundForEmoji = false,
                cornerSize = height,
                modifier = Modifier
                    .size(height)
            )
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxHeight()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }


        // center content
        Column(
            modifier = Modifier
                .constrainAs(center) {
                    linkTo(
                        start = image.end,
                        end = expand.start,
                        top = parent.top,
                        bottom = image.bottom,
                        startMargin = 16.dp,
                        endMargin = 8.dp,
                        topMargin = 2.dp,
                        bottomMargin = 2.dp,
                        horizontalBias = 0f
                    )
                    width = Dimension.preferredWrapContent
                },
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
                        painter = AppIcons.Group,
                        modifier = Modifier.size(14.dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(4.dp))

                    val text = if (inactiveUserCount == 0) "**$userCount**"
                    else {
                        quantityStringResource(
                            id = R.plurals.group_sharing_member_active_info,
                            quantity = inactiveUserCount, userCount, inactiveUserCount
                        )
                    }
                    Text(
                        text = text.annotatedBold(),
                        style = MaterialTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                    )
                }
            }
        }

        // expand icon
        Box(
            modifier = Modifier
                .constrainAs(expand) {
                    end.linkTo(parent.end)
                    bottom.linkTo(image.bottom)
                    top.linkTo(image.top)
                    this.height = Dimension.fillToConstraints
                    this.width = Dimension.ratio("1:1")
                }
                .padding(4.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(percent = 100)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (verified) {
                Icon(
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    painter = AppIcons.Warning,
                    tint = MaterialTheme.colors.onError,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(device = Devices.PIXEL_3A)
@Composable
fun MemberPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(modifier = Modifier.padding(40.dp)) {
                MapGroupPicker(
                    groupName = "Robots in da house",
                    groupImage = ImageModel.emoji(Emojis.robot),
                    verified = false,
                    userCount = 5,
                    inactiveUserCount = 2,
                    onExpandClick = {},
                    onGroupClick = {}
                )
            }
        }
    }
}