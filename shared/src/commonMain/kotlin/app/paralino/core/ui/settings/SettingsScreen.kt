package app.paralino.core.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.settings.AccountScreenEvent
import app.paralino.core.presentation.settings.AccountScreenUiEvent
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.billing.PremiumPlanBanner
import app.paralino.core.ui.settings.profile.AccountInfoHeader
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreenCore(
    appState: CoreUiState,
    viewModel: SettingsViewModel,
    onShareApp: (String) -> Unit,
    openConvertAccount: () -> Unit,
    openProfileDetails: () -> Unit,
    openPersonalCodes: () -> Unit,
    openBlockList: () -> Unit,
    openGeneralInfo: () -> Unit,
    openAppStatus: () -> Unit,
    openSubscriptions: () -> Unit,
    openPaywall: () -> Unit,
    openLocationSettings: () -> Unit,
    openAppSettings: () -> Unit,
    askNotificationPermission: () -> Unit,
    onShareCode: (String) -> Unit,
) {

    MessageDispatcherEffect(viewModel, appState)
    val accountState by viewModel.accountState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(key1 = Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AccountScreenEvent.EmailVerificationSent -> {}

                AccountScreenEvent.OpenAppReview -> {
                    accountState.data?.appReviewUrl?.let { uriHandler.openUri(it) }
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    val dialog = accountState.data?.alertDialog
    dialog?.let { dialog ->
        dialog.Show {
            viewModel.onEvent(AccountScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 60.dp)
    ) {
        val readyState = accountState.data
        if (readyState != null) {
            AccountInfoHeader(
                name = readyState.userState.name,
                image = readyState.userState.image ?: ImageModel.emoji(Emojis.robot),
                personalCode = readyState.userState.personalCode,
                isGuestUser = readyState.userState.isGuestUser,
                userPlan = readyState.userPlan,
                errors = readyState.errors,
                onGuestAccountClick = {
                    if (readyState.canUseEmail) {
                        openConvertAccount()
                    }
                },
                onErrorItemClick = {
                    when (it) {
                        is ConditionErrorType.EmailVerification -> {
                            viewModel.onEvent(AccountScreenUiEvent.SendEmailVerification)
                        }

                        is ConditionErrorType.PermissionNotifications -> {
                            askNotificationPermission()
                        }

                        is ConditionErrorType.NewAppUpdate -> {
                            viewModel.onEvent(AccountScreenUiEvent.TriggerAppUpdate)
                        }

                        else -> {}
                    }
                },
                onCopy = { viewModel.onEvent(AccountScreenUiEvent.Copy(it)) },
                onShareCode = onShareCode,
                onClick = openPersonalCodes
            )
        }

        AnimatedVisibility(visible = readyState?.userPlan?.planTypeEnum?.isPremium == false) {
            PremiumPlanBanner(
                modifier = Modifier.padding(top = 20.dp),
                onClick = openPaywall
            )
        }

        Spacer(modifier = Modifier.size(20.dp))

        if (readyState != null) {
            SettingsActionItem(
                name = stringResource(MR.strings.settings_section_account_title),
                icon = CoreAppIcons.Person.asPainterResource()
            ) {
                openProfileDetails()
            }

            SettingsActionItem(
                name = stringResource(MR.strings.plans_management_screen_title),
                icon = CoreAppIcons.StarFamily.asPainterResource()
            ) {
                openSubscriptions()
            }

            SettingsActionItem(
                name = stringResource(MR.strings.setting_appSettings_title),
                icon = CoreAppIcons.Settings.asPainterResource()
            ) {
                openAppSettings()
            }

            SettingsActionItem(
                name = stringResource(MR.strings.setting_location_title),
                icon = CoreAppIcons.ShareLocation.asPainterResource()
            ) {
                openLocationSettings()
            }

            if (readyState.canBoostLocation) {
                SettingsActionItem(
                    name = stringResource(MR.strings.setting_title_locationBoost),
                    description = when (readyState.boostTime != null) {
                        true -> stringResource(
                            MR.strings.setting_description_locationBoost_enabled,
                            readyState.boostTime
                        )

                        false -> stringResource(MR.strings.setting_description_locationBoost_disabled)
                    },
                    icon = CoreAppIcons.ShareLocation.asPainterResource(),
                    type = SettingsActionItemType.Switch(readyState.boostTime != null)
                ) {
                    viewModel.onEvent(AccountScreenUiEvent.ToggleLocationBoost)
                }
            }
        }

        if (readyState?.canAccessDebugScreen == true) {
            SettingsActionItem(
                name = "App status",
                icon = CoreAppIcons.Settings.asPainterResource()
            ) {
                openAppStatus()
            }
        }

        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_share),
            icon = CoreAppIcons.Share.asPainterResource()
        ) {
            onShareApp(BackendResourceIDs.downloadUrl)
        }

        AnimatedVisibility(
            visible = readyState?.isAppReviewVisible == true
        ) {
            SettingsActionItem(
                name = stringResource(MR.strings.setting_appReview_title),
                icon = CoreAppIcons.StarShine.asPainterResource(),
                iconTint = AppColors.highlightGold,
                tag = stringResource(MR.strings.setting_appReview_info),
                type = SettingsActionItemType.Action
            ) {
                viewModel.onEvent(AccountScreenUiEvent.TryToOpenAppReview)
            }
        }

        AnimatedVisibility(
            visible = readyState?.isAppReviewExpanded == true && readyState.isAppReviewVisible
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledButton(
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.SMALL_ACTION(
                        paddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        minContentHeight = 16.dp,
                        shape = RoundedCornerShape(100),
                        iconSize = 18.dp,
                        iconSpacing = 8.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.onSurface
                    ),
                    elevation = 0.dp,
                    onClick = {
                        viewModel.onEvent(AccountScreenUiEvent.CloseAppReview)
                    }
                ) {
                    Text(
                        text = stringResource(MR.strings.button_no),
                        style = MaterialTheme.typography.button
                    )
                }

                FilledButton(
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.SMALL_ACTION(
                        paddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        minContentHeight = 16.dp,
                        shape = RoundedCornerShape(100),
                        iconSize = 18.dp,
                        iconSpacing = 8.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.onSurface
                    ),
                    elevation = 0.dp,
                    onClick = {
                        viewModel.onEvent(AccountScreenUiEvent.OpenAppReview)
                    }
                ) {
                    Text(
                        text = stringResource(MR.strings.button_yes),
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }

        SettingsActionItem(
            name = stringResource(MR.strings.settings_section_general_title),
            icon = CoreAppIcons.LibraryBooks.asPainterResource()
        ) {
            openGeneralInfo()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsActionItem(
    modifier: Modifier = Modifier,
    name: String,
    icon: Painter,
    tag: String? = null,
    description: String? = null,
    nameTextStyle: TextStyle = MaterialTheme.typography.subtitle2.copy(fontSize = 18.sp),
    iconBackgroundTint: Color = MaterialTheme.colors.surface,
    iconTint: Color = MaterialTheme.colors.onSurface,
    type: SettingsActionItemType = SettingsActionItemType.Navigation,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(enabled = onClick != null || onLongClick != null, onLongClick = onLongClick, onClick = { onClick?.invoke() })
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                tint = iconTint,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = iconBackgroundTint,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    modifier = Modifier,
                    text = name,
                    style = nameTextStyle,
                    textAlign = TextAlign.Start
                )
                if (tag != null) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier,
                            text = tag.annotatedBoldCompose(),
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            when (type) {
                SettingsActionItemType.Action -> {}
                SettingsActionItemType.Navigation -> {
                    Icon(
                        modifier = Modifier.width(20.dp),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                        contentDescription = null
                    )
                }
                SettingsActionItemType.Link -> {
                    Icon(
                        modifier = Modifier.width(20.dp),
                        painter = CoreAppIcons.ArrowOpenNew.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                        contentDescription = null
                    )
                }

                is SettingsActionItemType.Switch -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                    ) {
                        if (type.checked) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isSystemInDarkTheme()) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary,
                                        shape = CircleShape
                                    )
                                    .padding(1.dp),
                                painter = CoreAppIcons.CheckCircle.asPainterResource(),
                                contentDescription = null,
                                tint = if (isSystemInDarkTheme()) MaterialTheme.colors.primary else MaterialTheme.colors.onPrimary,
                            )
                        } else {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                painter = CoreAppIcons.RadioButtonUnchecked.asPainterResource(),
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.disabled)
                            )
                        }
                    }
                }

                SettingsActionItemType.Loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onBackground,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(22.dp)
                    )
                }
            }
        }
        if (description != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .padding(horizontal = 4.dp),
                    text = description.annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

sealed interface SettingsActionItemType {
    object Navigation : SettingsActionItemType
    object Action : SettingsActionItemType
    object Link : SettingsActionItemType
    object Loading : SettingsActionItemType
    data class Switch(val checked: Boolean) : SettingsActionItemType
}