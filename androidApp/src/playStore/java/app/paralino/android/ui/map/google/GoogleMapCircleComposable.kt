package app.paralino.android.ui.map.google

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.ui.theme.AppColors
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle

@Composable
fun GoogleMapCircleComposable(
    model: MapPinData,
    isSatelliteMode: Boolean,
    latValue: Double,
    lngValue: Double,
    accuracy: Double,
    onClick: () -> Unit
) {
    if (model.shouldShowApproximateCircle) {
        Circle(
            center = LatLng(latValue, lngValue),
            clickable = true,
            fillColor = when (model.pinType) {
                MapPinType.MEMBER -> AppColors.accentDark.copy(alpha = 0.5f)

                MapPinType.PLACE -> {
                    if (isSatelliteMode) {
                        Color.Blue.copy(alpha = 0.2f)
                    } else {
                        if (isSystemInDarkTheme()) {
                            AppColors.accent.copy(alpha = 0.15f)
                        } else {
                            Color.Blue.copy(alpha = 0.15f)
                        }
                    }
                }

                MapPinType.PRIVACY_ZONE -> {
                    if (isSatelliteMode) {
                        Color.Black.copy(alpha = 0.4f)
                    } else {
                        if (isSystemInDarkTheme()) {
                            AppColors.accentDark.copy(alpha = 0.25f)
                        } else {
                            Color.Black.copy(alpha = 0.25f)
                        }
                    }
                }
            },
            radius = accuracy,
            strokeColor = when (model.pinType) {
                MapPinType.MEMBER -> Color.White
                MapPinType.PLACE -> Color.Blue
                MapPinType.PRIVACY_ZONE -> {
                    if (isSystemInDarkTheme() || isSatelliteMode) {
                        AppColors.redMain
                    } else {
                        Color.Black
                    }
                }
            },
            strokeWidth = when (model.pinType) {
                MapPinType.MEMBER -> 8f
                MapPinType.PLACE -> 0f
                MapPinType.PRIVACY_ZONE -> 8f
            },
            strokePattern = listOf(
                Dash(20f),
                Gap(12f)
            ),
            tag = model.markerId,
            zIndex = if (model.focused) 11f else 10f,
            onClick = { onClick() }
        )
    }
}