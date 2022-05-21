package com.zbapp.locationtrack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout

object LargeSwitchDefaults {
    val minWidth = 160.dp
    val minHeight = 120.dp
}

@Composable
fun LargeSwitch(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String? = null,
    state: Boolean,
    onClick: (Boolean) -> Unit,
) {

    val sourceColor = if (state) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val backgroundColor by animateColorAsState(targetValue = sourceColor)
    val contentColor by animateColorAsState(targetValue = contentColorFor(backgroundColor = sourceColor))

    ConstraintLayout(
        modifier = modifier
            .defaultMinSize(minHeight = LargeSwitchDefaults.minHeight, minWidth = LargeSwitchDefaults.minWidth)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick(state) }
            .padding(16.dp),
    ) {

        val (iconRef, titleRef, infoRef, indicatorRef) = createRefs()

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .constrainAs(iconRef) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                }
                .size(36.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            tint = contentColor
        )

        Text(
            text = title,
            style = MaterialTheme.typography.caption,
            color = contentColor,
            modifier = Modifier.constrainAs(titleRef) {
                bottom.linkTo(infoRef.top)
                start.linkTo(parent.start)
            }
        )

        Box(modifier = Modifier.constrainAs(infoRef) {
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
        }
        ) {
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                    color = contentColor.copy(alpha = ContentAlpha.disabled)
                )
            }
        }

        Box(modifier = Modifier
            .constrainAs(indicatorRef) {
                end.linkTo(parent.end)
                top.linkTo(iconRef.top)
                bottom.linkTo(iconRef.bottom)
            }
            .size(20.dp)
//            .border(1.dp, contentColor, CircleShape)
        ) {
            if (state)
                Icon(modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = contentColor)
            else
                Icon(modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = ContentAlpha.disabled))
        }

    }
}