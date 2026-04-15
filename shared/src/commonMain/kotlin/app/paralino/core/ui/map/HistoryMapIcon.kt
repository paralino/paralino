package app.paralino.core.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.conditional

@Composable
fun HistoryMapIcon(
    isAccurate: Boolean,
    isClustered: Boolean
) {
    Icon(
        modifier = Modifier
            .size(
                if (isClustered) {
                    24.dp
                } else if (isAccurate) {
                    16.dp
                } else {
                    28.dp
                }
            )
            .conditional(isClustered) {
                background(
                    color = AppColors.lightSurface,
                    shape = CircleShape
                )
            },
        painter = if (isClustered) {
            CoreAppIcons.CircleCircle.asPainterResource()
        } else if (isAccurate) {
            CoreAppIcons.LocationOnFilled.asPainterResource()
        } else {
            CoreAppIcons.NotListedLocationFilled.asPainterResource()
        },
        contentDescription = null,
        tint = if (isClustered) {
            AppColors.redMain
        } else {
            Color.Unspecified
        }
    )
}