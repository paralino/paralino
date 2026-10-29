package app.paralino.core.ui.settings.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.settings.AccountScreenUiEvent
import app.paralino.core.presentation.settings.SettingsActionItemType
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ComponentInfoBox
import app.paralino.core.ui.components.ComponentInfoPoint
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
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
    openUserDevices: () -> Unit,
    askNotificationPermission: () -> Unit,
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
                    errors = state.data.errors,
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
                    name = stringResource(MR.strings.settings_account_devices_screen_title),
                    icon = CoreAppIcons.PhoneAndroid.asPainterResource()
                ) {
                    openUserDevices()
                }

                SettingsActionItem(
                    name = stringResource(MR.strings.blocklist_screen_title),
                    icon = CoreAppIcons.Block.asPainterResource()
                ) {
                    openBlockList()
                }

                SettingsActionItem(
                    name = stringResource(MR.strings.settings_account_edit_profile),
                    icon = CoreAppIcons.Person.asPainterResource()
                ) {
                    openEditProfile()
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

                Divider(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 20.dp)
                        .padding(horizontal = 30.dp),
                    color = Color.Gray.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )

                AccountInfoBox(
                    modifier = Modifier.padding(top = 32.dp),
                    email = state.data.email,
                    userId = state.data.userId,
                    billingId = state.data.billingId,
                    deviceId = state.data.deviceId,
                    diagnosticsId = state.data.diagnosticsId,
                    pushToken = state.data.pushToken,
                    memberSinceDateFormatted = state.data.memberSinceDateFormatted,
                    onContentCopy = { viewModel.onEvent(AccountScreenUiEvent.Copy(it)) }
                )
            }

            Payload.Loading -> {}
            is Payload.Error -> {}
        }
    }

    if (accountState.data?.loadingAction == true) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}

@Composable
private fun AccountInfoBox(
    modifier: Modifier = Modifier,
    email: String?,
    userId: String,
    billingId: String,
    deviceId: String,
    diagnosticsId: String,
    pushToken: String?,
    memberSinceDateFormatted: String,
    onContentCopy: (String) -> Unit
) {
    ComponentInfoBox(
        modifier = modifier,
        items = listOfNotNull(
            email?.let { email ->
                ComponentInfoPoint(
                    content = stringResource(MR.strings.settings_account_info_email, email)
                )
            },
            ComponentInfoPoint(
                content = stringResource(MR.strings.settings_account_info_user_id, userId)
            ),
            ComponentInfoPoint(
                content = stringResource(MR.strings.settings_account_info_billing_id, billingId)
            ),
            ComponentInfoPoint(
                content = stringResource(MR.strings.settings_account_info_device_id, deviceId)
            ),
            ComponentInfoPoint(
                content = stringResource(MR.strings.settings_account_info_diagnostics_id, diagnosticsId)
            ),
            pushToken?.let { pushToken ->
                ComponentInfoPoint(
                    content = stringResource(MR.strings.settings_account_info_push_token, pushToken)
                )
            },
        ),
        footerInfoText = stringResource(MR.strings.settings_account_info_member_since, memberSinceDateFormatted),
        onCopy = onContentCopy,
    )
}