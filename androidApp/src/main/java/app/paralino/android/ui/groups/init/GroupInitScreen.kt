package app.paralino.android.ui.groups.init

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.*
import app.paralino.android.ui.invite.manage.PersonalCodeItem
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.settings.SettingsBigHeader
import app.paralino.android.ui.settings.SettingsSectionHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.android.utils.extensions.copy
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.groups.GroupInitScreenEvent
import app.paralino.core.presentation.groups.GroupInitScreenState
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel


@Composable
fun GroupInitScreen(
    appState: MainAppState,
    vm: GroupInitViewModel = getViewModel(),
    onClose: () -> Unit,
) {

    val vmState by vm.uiState.collectAsState(GroupInitScreenState())
    val state = vmState

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val context = LocalContext.current

    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is GroupInitScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                GroupInitScreenEvent.EmailVerificationSent -> {
                    appState.showSnackbar(context.getString(R.string.info_email_check_instructions))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.VerifyEmail -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.sendEmailVerification()
                }
            }
        }
        else -> {}
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    ImageModelImage(
                        imageModel = state.userImage,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { appState.navController.navigate(AppScreen.Settings.routeSignature) }
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(it)
        ) {

            if (state.errors.isNotEmpty())
                Spacer(modifier = Modifier.size(20.dp))

            state.errors.forEach {
                ConditionErrorItem(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    data = it
                ) {
                    when (it) {
                        is ConditionErrorType.EMAIL_VERIFICATION -> showDialog =
                            MainAlertDialog.VerifyEmail(it.email)
                        ConditionErrorType.NETWORK -> appState.showSnackbar(
                            text = context.getString(R.string.error_no_connection_snackbar)
                        )
                        else -> { /* do nothing */
                        }
                    }
                }
            }

            SettingsBigHeader(
                modifier = Modifier
                    .padding(horizontal = 20.dp),
                name = stringResource(id = R.string.group_create_start_title)
            )

            Spacer(modifier = Modifier.size(20.dp))

            LargeSeekbarSwitch(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .height(LargeSwitchDefaults.minHeight)
                    .fillMaxWidth(1f),
                icon = AppIcons.Add,
                title = stringResource(id = R.string.dashboard_groups_add_button),
                titleStyle = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = AppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    appState.navController.navigate(AppScreen.MainHome.GroupCreate.route())
                }
            )

            Spacer(modifier = Modifier.size(20.dp))

            if (state.personalCode != null) {
                SettingsSectionHeader(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    name = stringResource(id = R.string.dashboard_groups_join_info)
                )
                PersonalCodeItem(
                    state = state.personalCode!!,
                    deleteLoading = false,
                    onCopy = {
                        context.copy(state.personalCode?.code ?: "")
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_copy_invite_code),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onShare = {
                        context.share(state.personalCode?.code ?: "")
                    },
                    onDelete = {}
                )
            } else {
                LargeSeekbarSwitch(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .height(LargeSwitchDefaults.minHeight)
                        .fillMaxWidth(1f),
                    icon = AppIcons.Key,
                    title = stringResource(id = R.string.dashboard_groups_join_button),
                    titleStyle = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal),
                    description = null,
                    state = LargeSeekbarSwitchState.SeekbarSwitchState(
                        switchState = LargeSeekbarSwitchState.Switch(
                            checkState = false,
                            unCheckedIconRes = AppIcons.ChevronRight
                        ),
                        seekbarState = null
                    ),
                    onStateChange = {
                        appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
                    }
                )
            }

            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}
