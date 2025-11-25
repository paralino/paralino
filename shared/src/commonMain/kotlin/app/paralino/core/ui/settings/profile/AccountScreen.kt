package app.paralino.core.ui.settings.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.data
import app.paralino.core.presentation.settings.AccountScreenUiEvent
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.settings.SettingsActionItemType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AccountScreenCore(
    appState: CoreUiState,
    viewModel: SettingsViewModel,
    openProfileRawScreen: () -> Unit,
    openConvertAccount: () -> Unit,
    openEditProfile: () -> Unit,
    openChangeEmail: () -> Unit,
    openChangePassword: () -> Unit,
    openBlockList: () -> Unit,
    onShare: (String) -> Unit,
) {

    MessageDispatcherEffect(viewModel, appState)
    var showDangerZoneContent by remember { mutableStateOf(false) }
    val accountState by viewModel.accountState.collectAsState()

    val dialog = accountState.data?.alertDialog
    dialog?.Show {
        viewModel.onEvent(AccountScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 60.dp)
    ) {
        when (val state = accountState) {
            is Payload.Success -> {
                AccountInfoHeader(
                    name = state.data.userState.name,
                    image = state.data.userState.image ?: ImageModel.emoji(Emojis.robot),
                    personalCode = state.data.userState.personalCode,
                    isGuestUser = state.data.userState.isGuestUser,
                    errorItem = state.data.userState.errorItem,
                    userPlan = state.data.userPlan,
                    onGuestAccountClick = {
                        if (state.data.canUseEmail) {
                            openConvertAccount()
                        }
                    },
                    onErrorItemClick = {
                        when (it) {
                            is ConditionErrorType.EmailVerification -> {
                                viewModel.onEvent(AccountScreenUiEvent.SendEmailVerification)
                            }

                            else -> {}
                        }
                    },
                    onCopy = { viewModel.onEvent(AccountScreenUiEvent.Copy(it)) },
                    onShareCode = { onShare(it) },
                    onClick = null
                )

                Spacer(modifier = Modifier.size(20.dp))

                SettingsActionItem(
                    name = stringResource(MR.strings.raw_data_screen_title),
                    icon = CoreAppIcons.Security.asPainterResource()
                ) {
                    openProfileRawScreen()
                }

                SettingsActionItem(
                    name = stringResource(MR.strings.settings_account_edit_profile),
                    icon = CoreAppIcons.Person.asPainterResource()
                ) {
                    openEditProfile()
                }

                SettingsActionItem(
                    name = stringResource(MR.strings.blocklist_screen_title),
                    icon = CoreAppIcons.Block.asPainterResource()
                ) {
                    openBlockList()
                }

                if (state.data.userState.isGuestUser) {
                    if (state.data.canUseEmail) {
                        SettingsActionItem(
                            name = stringResource(MR.strings.settings_account_guest_convert),
                            icon = CoreAppIcons.HowToReg.asPainterResource()
                        ) {
                            openConvertAccount()
                        }
                    }
                } else {
                    SettingsActionItem(
                        name = stringResource(MR.strings.settings_account_edit_email),
                        icon = CoreAppIcons.Email.asPainterResource()
                    ) {
                        openChangeEmail()
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.settings_account_edit_password),
                        icon = CoreAppIcons.Password.asPainterResource()
                    ) {
                        openChangePassword()
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.setting_title_logout),
                        icon = CoreAppIcons.Logout.asPainterResource(),
                        iconBackgroundTint = AppColors.errorBackground,
                        type = SettingsActionItemType.Action
                    ) {
                        viewModel.onEvent(AccountScreenUiEvent.Logout)
                    }
                }

                AnimatedVisibility(visible = showDangerZoneContent) {
                    Column {
                        SettingsSectionHeader(name = stringResource(MR.strings.setting_title_dangerZone))
                        SettingsActionItem(
                            name = stringResource(MR.strings.setting_title_delete_account),
                            icon = CoreAppIcons.DeleteForever.asPainterResource(),
                            iconBackgroundTint = AppColors.errorBackground,
                            type = SettingsActionItemType.Action
                        ) {
                            viewModel.onEvent(AccountScreenUiEvent.DeleteAccount)
                        }
                    }
                }

                AnimatedVisibility(visible = !showDangerZoneContent) {
                    SettingsActionItem(
                        name = stringResource(MR.strings.setting_title_dangerZone),
                        icon = CoreAppIcons.Warning.asPainterResource(),
                        iconBackgroundTint = AppColors.errorBackground,
                        type = SettingsActionItemType.Action
                    ) {
                        showDangerZoneContent = true
                    }
                }
            }

            Payload.Loading -> {}
            is Payload.Error -> {}
        }
    }

    if (accountState.data?.loadingAction == true) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountInfoHeader(
    modifier: Modifier = Modifier,
    name: String,
    image: ImageModel,
    personalCode: String?,
    isGuestUser: Boolean,
    errorItem: ConditionErrorType?,
    userPlan: PlanConfig,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onGuestAccountClick: () -> Unit,
    onCopy: (String) -> Unit,
    onClick: (() -> Unit)?,
    onShareCode: (String) -> Unit,
) {
    Column {
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
                                bottom = if (isGuestUser || userPlan.planTypeEnum.isOtherThanSilver) 10.dp else 0.dp
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

                        if (userPlan.planTypeEnum.isOtherThanSilver) {
                            AccountHeaderChip(
                                backgroundColor = AppColors.accentDark,
                                contentColor = MaterialTheme.colors.onPrimary,
                                icon = null,
                                text = userPlan.planName,
                                onClick = null
                            )
                        }
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
                            modifier = Modifier
                        ) {
                            FilledButton(
                                modifier = modifier.size(32.dp),
                                onClick = { onShareCode(DeeplinkType.ParalinoID.buildUrl(personalCode)) },
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

        if (errorItem != null) {
            Spacer(modifier = Modifier.size(20.dp))
            ConditionErrorItem(
                data = errorItem
            ) {
                onErrorItemClick(errorItem)
            }
        }
    }
}

@Composable
fun AccountHeaderChip(
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