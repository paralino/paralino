package app.paralino.core.ui.invite.manage

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

@Composable
fun PersonalCodeItem(
    modifier: Modifier = Modifier,
    state: PersonalCodeScreenCodeData,
    hintDelayMillis: Long = 0,
    onCopy: () -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit,
    onActivate: (String) -> Unit = {},
) {
    if (state.isValid) {
        PersonalCodeItem(
            modifier = modifier,
            hintDelayMillis = hintDelayMillis,
            code = state.code,
            userImage = state.userImage,
            userName = state.userName,
            hasDelete = state.canDisable,
            deleteLoading = state.isActionLoading,
            onCopy = onCopy,
            onShare = onShare,
            onDelete = onDelete
        )
    } else {
        PersonalCodeItemInactive(
            modifier = modifier,
            state = state,
            onActivate = onActivate
        )
    }
}

@Composable
fun PersonalCodeItemInactive(
    modifier: Modifier = Modifier,
    state: PersonalCodeScreenCodeData,
    onActivate: (String) -> Unit,
) {
    ConditionErrorItem(
        modifier = modifier.padding(horizontal = 20.dp),
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
        icon = null,
        text = state.code,
        endAction = stringResource(resource = MR.strings.invite_code_button_activate),
        endActionLoading = state.isActionLoading,
        onClick = {
            onActivate(state.code)
        }
    )
}

@Composable
fun PersonalCodeItem(
    modifier: Modifier = Modifier,
    hintDelayMillis: Long,
    code: String,
    userImage: ImageModel,
    userName: String,
    hasDelete: Boolean,
    deleteLoading: Boolean,
    onCopy: () -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var animatedInfoText by rememberSaveable { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(hintDelayMillis)
        animatedInfoText = 1f
    }

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .zIndex(1f)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colors.primary,
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 20.dp)
                        .align(Alignment.CenterHorizontally),
                    text = code,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = 3.sp,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center,
                        lineBreak = LineBreak.Heading
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.size(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImageModelImage(
                        imageModel = userImage,
                        modifier = Modifier.size(60.dp),
                        useBackgroundForEmoji = false
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = userName, style = MaterialTheme.typography.h6)
                }

                Spacer(modifier = Modifier.size(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledButton(
                        style = ButtonStyle.SMALL,
                        icon = CoreAppIcons.ContentCopy.asPainterResource(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        onClick = onCopy
                    ) {
                        Text(text = stringResource(MR.strings.invite_code_button_copy))
                    }
                    FilledButton(
                        style = ButtonStyle.SMALL,
                        icon = CoreAppIcons.Share.asPainterResource(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        onClick = {
                            onShare(DeeplinkType.ParalinoID.buildUrl(code))
                        }
                    ) {
                        Text(text = stringResource(MR.strings.invite_code_button_share))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (hasDelete) {
                        IconButton(
                            onClick = { if (!deleteLoading) onDelete() }
                        ) {
                            if (deleteLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colors.onPrimary
                                )
                            } else {
                                Icon(
                                    painter = CoreAppIcons.Block.asPainterResource(),
                                    tint = MaterialTheme.colors.error,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier
                    .zIndex(0f)
                    .offset(y = animateDpAsState((1f - animatedInfoText).times(-200).dp).value)
                    .padding(horizontal = 30.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(bottomEnd = 12.dp, bottomStart = 12.dp)
                    )
                    .clip(RoundedCornerShape(bottomEnd = 12.dp, bottomStart = 12.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(top = 12.dp, bottom = 8.dp)
                    .padding(horizontal = 8.dp),
                text = stringResource(MR.strings.invite_manage_description).annotatedBold(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Spacer(modifier = Modifier.size(50.dp))
    }
}


@Composable
fun PersonalCodeItemWithHints(
    modifier: Modifier = Modifier,
    hintDelayMillis: Long = 0,
    personalCodeItem: @Composable Modifier.() -> Unit
) {
    var animatedCardInfo by rememberSaveable { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(hintDelayMillis)
        animatedCardInfo = 1f
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(MR.strings.dashboard_paralino_id_title).annotatedBold(),
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .alpha(animateFloatAsState(animatedCardInfo).value)
                .padding(horizontal = 20.dp)
        )
        DataInfoChip(
            modifier = Modifier
                .alpha(animateFloatAsState(animatedCardInfo).value)
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp),
            image = null,
            backgroundColor = AppColors.accentLight.copy(alpha = 0.3f),
            text = stringResource(MR.strings.dashboard_paralino_id_description)
        )
        Box(
            modifier = Modifier.padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            personalCodeItem(Modifier.padding(top = 40.dp))
            Icon(
                modifier = Modifier
                    .alpha(animateFloatAsState(animatedCardInfo).value)
                    .height(70.dp)
                    .rotate(-10f),
                painter = CoreAppIcons.ArrowSpinny.asPainterResource(),
                tint = AppColors.highlightGold,
                contentDescription = null
            )
        }
    }
}