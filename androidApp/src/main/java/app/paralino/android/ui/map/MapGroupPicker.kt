package app.paralino.android.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.android.utils.extensions.quantityStringResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.Emojis

@Composable
fun MapGroupPicker(
    modifier: Modifier = Modifier,
    groupName: String,
    groupImage: ImageModel,
    userCount: Int,
    inactiveUserCount: Int,
    onGroupClick: () -> Unit,
    onExpandClick: () -> Unit,
) {
    val height: Dp = remember { 56.dp }
    ConstraintLayout(
        modifier = modifier
            .padding(horizontal = 8.dp)
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
                .clickable { onExpandClick() },
            onDraw = {
                val pos = this.size.minDimension.div(2)
                val diff = height.plus(12.dp).div(height)
                val circlePath = Path().apply {
                    addOval(Rect(Offset(pos, pos), pos * diff))
                }
                clipPath(circlePath, clipOp = ClipOp.Difference) {
                    drawRoundRect(backgroundColor, cornerRadius = CornerRadius(pos, pos))
                }
            }
        )

        // image
        ImageModelImage(
            imageModel = groupImage,
            useBackgroundForEmoji = false,
            cornerSize = height,
            modifier = Modifier
                .size(height)
                .constrainAs(image) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = CircleShape
                )
                .clickable { onGroupClick() }
        )

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
                        imageVector = AppIcons.Group,
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
            Icon(
                imageVector = AppIcons.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
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
                    userCount = 5,
                    inactiveUserCount = 2,
                    onExpandClick = {},
                    onGroupClick = {}
                )
            }
        }
    }
}