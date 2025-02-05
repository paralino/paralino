package app.paralino.core.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.settings.AccountScreenUiEvent
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.settings.billing.PremiumPlanBanner
import app.paralino.core.ui.settings.profile.AccountInfoHeader
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun SettingsScreenCore(
    appState: CoreUiState,
    viewModel: SettingsViewModel,
    onShareApp: (String) -> Unit,
    openConvertAccount: () -> Unit,
    openProfileDetails: () -> Unit,
    openPersonalCodes: () -> Unit,
    openProfileRawScreen: () -> Unit,
    openBlockList: () -> Unit,
    openGeneralInfo: () -> Unit,
    openAppStatus: () -> Unit,
    openSubscriptions: () -> Unit,
    openPaywall: () -> Unit,
    openLocationSettings: () -> Unit,
    onShareCode: (String) -> Unit,
) {

    MessageDispatcherEffect(viewModel, appState)
    val accountState by viewModel.accountState.collectAsState()

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
                errorItem = null,
                onGuestAccountClick = {
                    if (readyState.canUseEmail) {
                        openConvertAccount()
                    }
                },
                onErrorItemClick = { },
                onCopy = { viewModel.onEvent(AccountScreenUiEvent.Copy(it)) },
                onShareCode = onShareCode,
                onClick = openPersonalCodes
            )
        }

        Spacer(modifier = Modifier.size(20.dp))

        AnimatedVisibility(visible = readyState?.userPlan?.planTypeEnum?.isPremium == false) {
            PremiumPlanBanner(
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
                name = stringResource(MR.strings.blocklist_screen_title),
                icon = CoreAppIcons.Block.asPainterResource()
            ) {
                openBlockList()
            }

            SettingsActionItem(
                name = stringResource(MR.strings.raw_data_screen_title),
                icon = CoreAppIcons.Security.asPainterResource()
            ) {
                openProfileRawScreen()
            }

            SettingsActionItem(
                name = stringResource(MR.strings.setting_location_title),
                icon = CoreAppIcons.ShareLocation.asPainterResource()
            ) {
                openLocationSettings()
            }

            SettingsActionItem(
                name = stringResource(MR.strings.plans_management_screen_title),
                icon = CoreAppIcons.StarFamily.asPainterResource()
            ) {
                openSubscriptions()
            }

            if (AppBuildConfig.instance.hasPreviewFeatures) {
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

            if (AppBuildConfig.instance.hasPreviewFeatures) {
                SettingsActionItem(
                    name = stringResource(MR.strings.setting_title_placeActivityThrottle),
                    description = when (readyState.placeActivityThrottleEnabled) {
                        true -> stringResource(MR.strings.setting_description_placeActivityThrottle_enabled)
                        false -> stringResource(MR.strings.setting_description_placeActivityThrottle_disabled)
                    },
                    icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                    type = SettingsActionItemType.Switch(readyState.placeActivityThrottleEnabled)
                ) {
                    viewModel.onEvent(AccountScreenUiEvent.TogglePlaceActivityThrottle)
                }
            }
        }

        SettingSectionInfo(
            onAppStatusClick = openAppStatus,
            onGeneralInfoClick = openGeneralInfo,
            onShareApp = onShareApp
        )
    }
}

@Composable
fun SettingSectionInfo(
    onGeneralInfoClick: () -> Unit,
    onAppStatusClick: () -> Unit,
    onShareApp: (String) -> Unit,
) {
    Column {
        if (AppBuildConfig.instance.hasPreviewFeatures) {
            SettingsActionItem(
                name = "App status",
                icon = CoreAppIcons.Settings.asPainterResource()
            ) {
                onAppStatusClick()
            }
        }

        if (AppBuildConfig.instance.hasPreviewFeatures) {
            SettingsActionItem(
                name = stringResource(MR.strings.setting_title_share),
                icon = CoreAppIcons.Share.asPainterResource()
            ) {
                onShareApp(BackendResourceIDs.homeUrl)
            }
        }

        SettingsActionItem(
            name = stringResource(MR.strings.settings_section_general_title),
            icon = CoreAppIcons.LibraryBooks.asPainterResource()
        ) {
            onGeneralInfoClick()
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
    iconTint: Color = MaterialTheme.colors.surface,
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
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = iconTint,
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
                    style = MaterialTheme.typography.subtitle2.copy(fontSize = 18.sp),
                    textAlign = TextAlign.Start
                )
                if (tag != null) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier,
                            text = tag.annotatedBold(),
                            style = MaterialTheme.typography.caption,
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
                        modifier = Modifier.width(24.dp),
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
            }
        }
        if (description != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .padding(horizontal = 4.dp),
                    text = description.annotatedBold(),
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
    data class Switch(val checked: Boolean) : SettingsActionItemType
}