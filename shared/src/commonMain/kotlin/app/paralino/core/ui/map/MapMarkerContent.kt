package app.paralino.core.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.provider.current
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun MapMarkerContent(
    modifier: Modifier = Modifier,
    state: MapPinData
) {
    Box(
        modifier = modifier.size(state.getPinSize().dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (state.markerStyle == MapMarkerStyle.PIN) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(MR.images.ic_pin_bubble),
                contentDescription = null,
                colorFilter = if (state.active) ColorFilter.tint(AppColors.accentDark) else ColorFilter.tint(
                    Color.DarkGray)
            )
        }
        val maxSize = when (state.markerStyle) {
            MapMarkerStyle.PIN -> 0.9f
            MapMarkerStyle.CIRCLE -> 1f
        }
        Box(
            modifier = Modifier.fillMaxSize(maxSize),
            contentAlignment = Alignment.Center
        ) {
            // when rendering on iOS size is not the same, probably because of bubble image from moko
            ImageModelImage(
                modifier = Modifier.fillMaxSize(
                    when (DeviceType.current){
                        DeviceType.Android -> 0.9f
                        DeviceType.iOS -> 0.7f
                    }
                ),
                imageModel = state.image ?: ImageModel.emoji(Emojis.lock),
                clipShape = CircleShape,
                colorFilter = if (!state.active) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                } else {
                    null
                }
            )
        }
    }
}