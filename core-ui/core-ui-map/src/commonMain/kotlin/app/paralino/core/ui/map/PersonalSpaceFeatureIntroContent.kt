package app.paralino.core.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.WavyDashedLine
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
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
                color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colors.surface
                } else {
                    AppColors.accentLight.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(
                    topStart = 19.dp,
                    topEnd = 19.dp,
                    bottomStart = 30.dp,
                    bottomEnd = 30.dp
                )
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
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(MR.strings.personalSpace_sheet_introBanner_title),
                style = MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(MR.strings.personalSpace_sheet_introBanner_message),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )

            FilledButton(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                style = remember {
                    ButtonStyle.BIG(paddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp))
                },
                elevation = 0.dp,
                icon = null,
                onClick = { onEvent(MapHomeScreenUiEvent.SetupPersonalSpaceIntro) }
            ) {
                Text(
                    modifier = Modifier.weight(1f).padding(start = 18.dp),
                    text = stringResource(MR.strings.personalSpace_sheet_introBanner_setup_button),
                    textAlign = TextAlign.Center
                )

                Icon(
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(18.dp)
                )
            }

            Divider(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .padding(top = 16.dp),
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
                thickness = 0.5.dp
            )

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                text = stringResource(MR.strings.personalSpace_sheet_introBanner_groupsOnly_header).annotatedBoldCompose(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )

            FilledButton(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 16.dp
                ),
                style = remember {
                    ButtonStyle.BIG(paddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp))
                },
                elevation = 0.dp,
                icon = null,
                onClick = { onEvent(MapHomeScreenUiEvent.DisablePersonalSpaceIntro) }
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(start = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(MR.strings.personalSpace_sheet_introBanner_groupsOnly_button))
                    Text(
                        text = stringResource(MR.strings.personalSpace_sheet_introBanner_groupsOnly_message),
                        style = MaterialTheme.typography.overline.copy(
                            color = MaterialTheme.colors.secondary
                        ),
                    )
                }

                Icon(
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(18.dp)
                )
            }
        }
    }
}
