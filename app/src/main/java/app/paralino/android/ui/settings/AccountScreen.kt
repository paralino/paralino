package app.paralino.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.ConditionErrorItem
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.shared.errorString
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.settings.AccountScreenEvent
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun AccountScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {
    val scrollState = rememberScrollState()
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
    var showDangerZoneContent by remember { mutableStateOf(false) }
    val accountState by viewModel.userData.collectAsState(Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AccountScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (showDialog) {
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
        else -> {}
    }

    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        SettingsBigHeader(
            name = stringResource(id = R.string.settings_section_account_title),
            bottomPadding = if (accountState.data?.userState?.isGuestUser == true) 0.dp
            else dimensionResource(id = R.dimen.activity_side_margin_medium_large)
        )

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
                        appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.ConvertAccount.routeSignature)
                    },
                    onErrorItemClick = {

                    }
                )

                SettingsActionItem(
                    name = stringResource(id = R.string.settings_account_edit_profile),
                    icon = Icons.Rounded.Person
                ) {
                    appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.EditProfile.routeSignature)
                }

                if (state.data.userState.isGuestUser) {
                    SettingsActionItem(
                        name = stringResource(id = R.string.settings_account_guest_convert),
                        icon = Icons.Rounded.HowToReg
                    ) {
                        appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.ConvertAccount.routeSignature)
                    }
                } else {
                    SettingsActionItem(
                        name = stringResource(id = R.string.settings_account_edit_email),
                        icon = Icons.Rounded.Email
                    ) {
                        appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.ChangeEmail.routeSignature)
                    }
                    SettingsActionItem(
                        name = stringResource(id = R.string.settings_account_edit_password),
                        icon = Icons.Rounded.Password
                    ) {
                        appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.ChangePassword.routeSignature)
                    }
                    SettingsActionItem(
                        name = stringResource(id = R.string.setting_title_logout),
                        icon = Icons.Rounded.Logout
                    ) {
                        showDialog = MainAlertDialog.Logout
                    }
                }

                AnimatedVisibility(visible = showDangerZoneContent) {
                    Column {
                        SettingsSectionHeader(name = stringResource(id = R.string.setting_title_dangerZone))
                        SettingsActionItem(
                            name = stringResource(id = R.string.setting_title_delete_account),
                            icon = Icons.Rounded.DeleteForever
                        ) {
                            showDialog = MainAlertDialog.DeleteAccount
                        }
                    }
                }

                AnimatedVisibility(visible = !showDangerZoneContent) {
                    SettingsActionItem(
                        name = stringResource(id = R.string.setting_title_dangerZone),
                        icon = Icons.Rounded.Warning
                    ) {
                        showDangerZoneContent = true
                    }
                }
            }
            Payload.Loading -> {}
            is Payload.Error -> {}
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
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_incognito),
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
            if (!isGuestUser) {
                Text(
                    text = "Email: **$email**".annotatedBold(),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
                )
            }
            Text(
                text = "ID: **$userId**".annotatedBold(),
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
            )
        }

        Spacer(modifier = Modifier.size(10.dp))

        if(errorItem != null) {
            ConditionErrorItem(
                data = errorItem
            ) {
                onErrorItemClick(errorItem)
            }
        }

        Spacer(modifier = Modifier.size(20.dp))
    }
}