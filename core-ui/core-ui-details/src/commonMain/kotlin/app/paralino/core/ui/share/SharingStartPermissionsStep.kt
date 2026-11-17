package app.paralino.core.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.common.endActionTextRes
import app.paralino.core.presentation.common.nameRes
import app.paralino.core.presentation.common.resolvedNameRes
import app.paralino.core.presentation.share.SharingRequirementState
import app.paralino.core.presentation.share.SharingStartScreenUiEvent
import app.paralino.core.presentation.share.isSameRequirementAs
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.icon
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharingStartPermissionsStep(
    modifier: Modifier = Modifier,
    visible: Boolean,
    requirements: List<SharingRequirementState>,
    activeRequirement: ConditionErrorType?,
    isRequesting: Boolean,
    onEvent: (SharingStartScreenUiEvent) -> Unit
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        BaseOnboardingScreen(
            modifier = Modifier.fillMaxSize(),
            nextButton = @Composable {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG,
                    state = AppButtonState.CONTENT,
                    icon = null,
                    onClick = { onEvent(SharingStartScreenUiEvent.StartPermissionRequest(type = null)) }
                ) {
                    Text(text = stringResource(MR.strings.button_continue))
                }
            },
            skipButton = @Composable {
                OnboardingSkipButton(
                    skipButtonText = stringResource(MR.strings.button_notNow),
                    onSkip = { onEvent(SharingStartScreenUiEvent.SkipPermissions) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = paddingValues.calculateBottomPadding() + 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            color = AppColors.accent.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(44.dp),
                        painter = CoreAppIcons.ShareLocation.asPainterResource(),
                        tint = AppColors.accent,
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(MR.strings.groups_sharing_permissions_title),
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(MR.strings.groups_sharing_permissions_description).annotatedBoldCompose(),
                        style = MaterialTheme.typography.subtitle1,
                        textAlign = TextAlign.Center
                    )
                }

                NoticeInfoChip(
                    modifier = Modifier.padding(top = 20.dp),
                    text = stringResource(MR.strings.groups_sharing_encryption_info)
                )

                Divider(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    color = Color.Gray.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(20.dp))
                // with lookaheadscope we can animate position of items in column with key()
                // just like with animateItem in LazyColumn
                // and we can't use LazyColumn since parent already has vertical scroll
                LookaheadScope {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        requirements.forEach { requirement ->
                            key(requirement.type.requirementKey) {
                                SharingRequirementItem(
                                    modifier = Modifier.animateBounds(lookaheadScope = this@LookaheadScope),
                                    requirement = requirement,
                                    isActive = activeRequirement?.isSameRequirementAs(requirement.type) == true,
                                    onClick = if (!requirement.isResolved && !isRequesting) {
                                        {
                                            onEvent(SharingStartScreenUiEvent.StartPermissionRequest(requirement.type))
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// keep it stable while location permission or push notification service changes between its variants
private val ConditionErrorType.requirementKey: Any
    get() = when (this) {
        is ConditionErrorType.PermissionLocation -> ConditionErrorType.PermissionLocation::class
        is ConditionErrorType.PushNotificationService -> ConditionErrorType.PushNotificationService::class
        else -> this
    }

@Composable
private fun SharingRequirementItem(
    modifier: Modifier = Modifier,
    requirement: SharingRequirementState,
    isActive: Boolean,
    onClick: (() -> Unit)?
) {
    val type = requirement.type
    val resolvedNameRes = type.resolvedNameRes.takeIf { requirement.isResolved }
    val isResolved = resolvedNameRes != null

    ConditionErrorItem(
        modifier = modifier,
        backgroundColor = animateColorAsState(
            if (isResolved) AppColors.accent.copy(alpha = 0.15f) else AppColors.errorBackground
        ).value,
        contentColor = animateColorAsState(
            if (isResolved) AppColors.accent else AppColors.redMain
        ).value,
        icon = if (isResolved) CoreAppIcons.CheckCircle.asPainterResource() else type.icon,
        text = stringResource(resolvedNameRes ?: type.nameRes),
        endAction = if (isResolved) null else type.endActionTextRes?.let { stringResource(it) },
        endActionLoading = isActive,
        onClick = onClick
    )
}
