package app.paralino.core.ui.settings.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.DeeplinkType
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountInfoHeader(
    modifier: Modifier = Modifier.Companion,
    name: String,
    image: ImageModel,
    personalCode: String?,
    isGuestUser: Boolean,
    errors: List<ConditionErrorType>,
    userPlan: PlanConfig,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onGuestAccountClick: () -> Unit,
    onCopy: (String) -> Unit,
    onClick: (() -> Unit)?,
    onShareCode: (String) -> Unit,
) {
    Column {
        if (errors.isNotEmpty()) {
            errors.forEach { error ->
                ConditionErrorItem(
                    data = error,
                    onClick = { onErrorItemClick(error) }
                )
            }
            Spacer(modifier = Modifier.size(20.dp))
        }

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.primary,
            elevation = 16.dp
        ) {
            Row(
                modifier = modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                            .padding(
                                bottom = if (isGuestUser) 10.dp else 0.dp
                            ),
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (isGuestUser) {
                            AccountHeaderChip(
                                backgroundColor = AppColors.redMain,
                                contentColor = MaterialTheme.colors.onPrimary,
                                icon = CoreAppIcons.Incognito.asPainterResource(),
                                text = stringResource(MR.strings.settings_account_guest_badge),
                                onClick = onGuestAccountClick
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }

                        AccountHeaderChip(
                            backgroundColor = AppColors.accentDark,
                            contentColor = MaterialTheme.colors.onPrimary,
                            icon = null,
                            text = userPlan.planName,
                            onClick = null
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImageModelImage(
                            imageModel = image,
                            modifier = Modifier.size(60.dp),
                            useBackgroundForEmoji = false
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (personalCode != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.Companion
                        ) {
                            FilledButton(
                                modifier = modifier.size(32.dp),
                                onClick = {
                                    onShareCode(
                                        DeeplinkType.ParalinoID.buildUrl(
                                            personalCode
                                        )
                                    )
                                },
                                icon = CoreAppIcons.Share.asPainterResource(),
                                style = ButtonStyle.ICON,
                                elevation = 0.dp,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.Transparent,
                                    contentColor = MaterialTheme.colors.onPrimary
                                ),
                                content = null
                            )
                            Column {
                                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                                    Text(
                                        modifier = Modifier.padding(start = 4.dp),
                                        text = stringResource(MR.strings.settings_section_personal_code_title),
                                        style = TextStyle(
                                            fontWeight = FontWeight.Light,
                                            fontSize = 12.sp,
                                        )
                                    )
                                }
                                Text(
                                    modifier = Modifier.padding(start = 4.dp),
                                    text = personalCode,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 18.sp,
                                        letterSpacing = 2.sp,
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.Start,
                                        lineBreak = LineBreak.Heading
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (onClick != null) {
                    Icon(
                        modifier = Modifier.width(20.dp),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        tint = MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.disabled),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeaderChip(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    icon: Painter?,
    text: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .background(
                backgroundColor,
                RoundedCornerShape(percent = 50)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text = text.annotatedBoldCompose(),
            style = MaterialTheme.typography.caption.copy(color = contentColor)
        )
    }
}