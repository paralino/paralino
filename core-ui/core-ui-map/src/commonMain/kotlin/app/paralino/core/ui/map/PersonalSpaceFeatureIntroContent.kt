package app.paralino.core.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.WavyDashedLine
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PersonalSpaceFeatureIntroContent(
    modifier: Modifier = Modifier,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = AppColors.accentLight.copy(alpha = 0.2f),
                shape = RoundedCornerShape(19.dp)
            )
            .clip(RoundedCornerShape(19.dp))
    ) {
        WavyDashedLine(
            modifier = Modifier
                .matchParentSize(),
            lineColor = AppColors.accent.copy(alpha = 0.1f),
            dotColor = AppColors.accent.copy(alpha = 0.1f),
            waveAngle = 35f
        )

        Icon(
            painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(50.dp)
                .offset(x = (-10).dp, y = (-10).dp)
                .rotate(160f),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )

        Icon(
            painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(80.dp)
                .rotate(20f)
                .offset(x = 30.dp, y = (-10).dp),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )

        Icon(
            painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(70.dp)
                .rotate(-10f)
                .offset(x = (-30).dp, y = 10.dp),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            DefaultToolbar(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(MR.strings.personalSpace_sheet_introBanner_title),
                elevation = 0.dp,
                color = Color.Transparent,
                contentColor = MaterialTheme.colors.onBackground,
                navigationIcon = {
                    ToolbarButtons.Close { onEvent(MapHomeScreenUiEvent.ConfirmPersonalSpaceIntro) }
                }
            )

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                text = stringResource(MR.strings.personalSpace_sheet_introBanner_message),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )
        }
    }
}
