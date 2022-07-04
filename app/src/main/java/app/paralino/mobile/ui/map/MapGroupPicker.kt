package app.paralino.mobile.ui.map

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.Emojis
import app.paralino.mobile.R
import app.paralino.mobile.ui.components.ImageModelImage
import app.paralino.mobile.ui.theme.AppTheme
import app.paralino.mobile.utils.extensions.annotatedBold
import app.paralino.mobile.utils.extensions.quantityStringResource

@Composable
fun MapGroupPicker(
    modifier: Modifier = Modifier,
    groupName: String,
    groupImage: ImageModel,
    userCount: Int,
    inactiveUserCount: Int,
    onClick: () -> Unit,
) {
    val height: Dp = remember { 46.dp }
    ConstraintLayout(
        modifier = modifier
            .padding(horizontal = 30.dp)
            .shadow(1.dp, RoundedCornerShape(height))
            .clip(RoundedCornerShape(height))
            .background(MaterialTheme.colors.background)
            .border(
                0.5.dp,
                MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                RoundedCornerShape(height)
            )
            .clickable { onClick() }
            .padding(4.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {

        val (image, center, expand) = createRefs()

        // image
        ImageModelImage(
            imageModel = groupImage,
            useBackgroundForEmoji = false,
            modifier = Modifier
                .size(height)
                .constrainAs(image) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                }
                .background(
                    MaterialTheme.colors.surface,
                    RoundedCornerShape(height)
                )
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
                        startMargin = 8.dp,
                        endMargin = 8.dp,
                        horizontalBias = 0f
                    )
                    width = Dimension.preferredWrapContent
                },
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        modifier = Modifier.size(14.dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(4.dp))

                    val text = if (inactiveUserCount == 0) "$userCount"
                    else {
                        quantityStringResource(id = R.plurals.group_sharing_member_active_info,
                            quantity = inactiveUserCount, userCount, inactiveUserCount)
                    }
                    Text(
                        text = text.annotatedBold(),
                        style = MaterialTheme.typography.caption.copy(fontSize = 12.sp,
                            fontWeight = FontWeight.Medium),
                        modifier = Modifier
                    )
                }
            }
        }

        // expand icon
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .constrainAs(expand) {
                    end.linkTo(parent.end, margin = 12.dp)
                    bottom.linkTo(image.bottom)
                    top.linkTo(image.top)
                }
        )
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
                    inactiveUserCount = 2
                ) { }
            }
        }
    }
}