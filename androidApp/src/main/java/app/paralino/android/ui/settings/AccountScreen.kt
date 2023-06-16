package app.paralino.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.android.ui.components.Show
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.utils.extensions.copy
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.AccountScreenEvent
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.components.SettingsBigHeader
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun AccountScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
    var showDangerZoneContent by remember { mutableStateOf(false) }
    val accountState by viewModel.accountState.collectAsState(Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AccountScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                AccountScreenEvent.EmailVerificationSent -> {
                    appState.showSnackbar(context.getString(R.string.info_email_check_instructions))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.Logout -> {
            MainAlertDialog.Logout.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    viewModel.logout()
                }
            }
        }
        is MainAlertDialog.DeleteAccount -> {
            MainAlertDialog.DeleteAccount.Show {
                showDialog = when (it) {
                    MainAlertDialog.Buttons.buttonOk -> MainAlertDialog.DeleteAccountConfirm
                    else -> null
                }
            }
        }
        is MainAlertDialog.DeleteAccountConfirm -> {
            MainAlertDialog.DeleteAccountConfirm.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    viewModel.deleteAccount()
                }
            }
        }
        is MainAlertDialog.VerifyEmail -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    viewModel.sendEmailVerification()
                }
            }
        }
        else -> {}
    }

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsBigHeader(
                name = stringResource(id = R.string.settings_section_account_title),
                bottomPadding = if (accountState.data?.userState?.isGuestUser == true) 0.dp
                else dimensionResource(id = R.dimen.activity_side_margin_medium_large)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    appState.navController.navigate(
                        AppScreen.Settings.ProfileDetails.RawDataScreen.route(
                            RawPresentationDataType.UserPresentation
                        )
                    )
                }
            ) {
                Icon(
                    painter = AppIcons.Security,
                    tint = AppColors.accent,
                    contentDescription = null
                )
            }
        }

        when (val state = accountState) {
            is Payload.Success -> {
                AccountInfoHeader(
                    name = state.data.userState.name,
                    email = state.data.userState.email,
                    userId = state.data.userState.userId,
                    image = state.data.userState.image ?: ImageModel.emoji(Emojis.robot),
                    isGuestUser = state.data.userState.isGuestUser,
                    errorItem = state.data.userState.errorItem,
                    onGuestAccountClick = {
                        if(state.data.canUseEmail) {
                            appState.navController.navigate(
                                AppScreen.Settings.ProfileDetails.ConvertAccount.routeSignature
                            )
                        }
                    },
                    onErrorItemClick = {
                        when (it) {
                            is ConditionErrorType.EMAIL_VERIFICATION -> showDialog =
                                MainAlertDialog.VerifyEmail(state.data.userState.email)
                            else -> {}
                        }
                    }
                )

                SettingsActionItem(
                    name = stringResource(id = R.string.settings_account_edit_profile),
                    icon = CoreAppIcons.Person.asPainterResource()
                ) {
                    appState.navController.navigate(AppScreen.Settings.ProfileDetails.EditProfile.routeSignature)
                }

                if (state.data.userState.isGuestUser) {
                    if(state.data.canUseEmail) {
                        SettingsActionItem(
                            name = stringResource(id = R.string.settings_account_guest_convert),
                            icon = AppIcons.HowToReg
                        ) {
                            appState.navController.navigate(AppScreen.Settings.ProfileDetails.ConvertAccount.routeSignature)
                        }
                    }
                } else {
                    SettingsActionItem(
                        name = stringResource(id = R.string.settings_account_edit_email),
                        icon = AppIcons.Email
                    ) {
                        appState.navController.navigate(AppScreen.Settings.ProfileDetails.ChangeEmail.routeSignature)
                    }
                    SettingsActionItem(
                        name = stringResource(id = R.string.settings_account_edit_password),
                        icon = AppIcons.Password
                    ) {
                        appState.navController.navigate(AppScreen.Settings.ProfileDetails.ChangePassword.routeSignature)
                    }
                    SettingsActionItem(
                        name = stringResource(id = R.string.setting_title_logout),
                        icon = AppIcons.Logout,
                        iconTint = MaterialTheme.colors.error,
                        showEndArrow = false
                    ) {
                        showDialog = MainAlertDialog.Logout
                    }
                }

                AnimatedVisibility(visible = showDangerZoneContent) {
                    Column {
                        SettingsSectionHeader(name = stringResource(id = R.string.setting_title_dangerZone))
                        SettingsActionItem(
                            name = stringResource(id = R.string.setting_title_delete_account),
                            icon = AppIcons.DeleteForever,
                            iconTint = MaterialTheme.colors.error,
                            showEndArrow = false
                        ) {
                            showDialog = MainAlertDialog.DeleteAccount
                        }
                    }
                }

                AnimatedVisibility(visible = !showDangerZoneContent) {
                    SettingsActionItem(
                        name = stringResource(id = R.string.setting_title_dangerZone),
                        icon = AppIcons.Warning,
                        iconTint = MaterialTheme.colors.error,
                        showEndArrow = false
                    ) {
                        showDangerZoneContent = true
                    }
                }
            }
            Payload.Loading -> {}
            is Payload.Error -> {}
        }
    }

    AnimatedVisibility(
        accountState.data?.loadingAction == true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background.copy(alpha = 0.5f))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun AccountInfoHeader(
    modifier: Modifier = Modifier,
    name: String,
    email: String,
    userId: String,
    image: ImageModel,
    isGuestUser: Boolean,
    errorItem: ConditionErrorType?,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onGuestAccountClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        if (isGuestUser) {
            val backgroundColor = MaterialTheme.colors.error
            val contentColor = contentColorFor(backgroundColor = backgroundColor)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        backgroundColor,
                        RoundedCornerShape(percent = 50)
                    )
                    .clickable { onGuestAccountClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = CoreAppIcons.Incognito.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_account_guest_badge).annotatedBold(),
                    style = MaterialTheme.typography.caption.copy(color = contentColor)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
        }

        Spacer(modifier = Modifier.size(20.dp))

        ImageModelImage(
            modifier = Modifier.size(100.dp),
            imageModel = image
        )

        Spacer(modifier = Modifier.size(10.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.SemiBold)
        )

        Spacer(modifier = Modifier.size(10.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            if (!isGuestUser && email.isNotBlank()) {
                Text(
                    text = "Email: **$email**".annotatedBold(),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
                )
            }
            if (userId.isNotBlank()) {
                Text(
                    modifier = Modifier.clickable {
                        context.copy(userId)
                    },
                    text = "ID: **$userId**".annotatedBold(),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
                )
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        if (errorItem != null) {
            ConditionErrorItem(
                data = errorItem
            ) {
                onErrorItemClick(errorItem)
            }
        }

        Spacer(modifier = Modifier.size(20.dp))
    }
}