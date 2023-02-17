package app.paralino.android.ui.groups.init

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Key
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.ConditionErrorItem
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.invite.manage.PersonalCodeItem
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.settings.SettingsBigHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.ButtonStyle
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

    when(val dialog = showDialog) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SettingsBigHeader(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
            name = stringResource(id = R.string.group_create_start_title)
        )

        DashboardGroupJoinButtons(appState)

        if(state.personalCode !=  null) {
            PersonalCodeItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                state = state.personalCode!!,
                deleteLoading = false,
                onCopy = {  },
                onShare = {  },
                onDelete = {}
            )
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState)
        ) {
            state.errors.forEach {
                ConditionErrorItem(data = it) {
                    when (it) {
                        is ConditionErrorType.EMAIL_VERIFICATION -> showDialog = MainAlertDialog.VerifyEmail(it.email)
                        ConditionErrorType.NETWORK -> appState.showSnackbar(
                            text = context.getString(R.string.error_no_connection_snackbar)
                        )
                        else -> { /* do nothing */ }
                    }
                }
            }
        }
    }

}

@Composable
fun ColumnScope.DashboardGroupJoinButtons(appState: MainAppState) {
    FilledButton(
        style = ButtonStyle.SMALL,
        modifier = Modifier.align(Alignment.CenterHorizontally),
        icon = Icons.Rounded.GroupAdd,
        onClick = {
            appState.navController.navigate(AppScreen.MainHome.GroupCreate.route())
        }
    ) {
        Text(text = stringResource(id = R.string.dashboard_groups_add_button))
    }

    Spacer(modifier = Modifier.size(10.dp))

    FilledButton(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = ButtonStyle.SMALL,
        icon = Icons.Rounded.Key,
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        onClick = {
            appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
        }
    ) {
        Text(text = stringResource(id = R.string.dashboard_groups_join_button))
    }
}
