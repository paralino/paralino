package app.paralino.core.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage

@Composable
fun MapGroupPicker(
    modifier: Modifier = Modifier,
    groupImage: ImageModel,
    onExpandClick: () -> Unit,
) {
    val height: Dp = remember { 56.dp }
    Surface(
        modifier = modifier
            .height(height)
            .wrapContentWidth()
            .clickable(onClick = onExpandClick),
        elevation = 8.dp,
        shape = RoundedCornerShape(percent = 100),
        color = MaterialTheme.colors.background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageModelImage(
                imageModel = groupImage,
                useBackgroundForEmoji = false,
                clipShape = CircleShape,
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
                    painter = CoreAppIcons.ExpandMore.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
