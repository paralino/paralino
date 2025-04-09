package app.paralino.android.ui.invite.manage

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.utils.extensions.share
import app.paralino.core.MR
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.invite.manage.PersonalCodeScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PersonalCodeScreen(
    appState: MainAppState,
    vm: PersonalCodeViewModel = koinViewModel(),
) {

    val context = LocalContext.current
    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_section_personal_code_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        PersonalCodeScreenCore(
            appState = appState,
            vm = vm,
            askNotificationPermission = {
                notificationPermissionHandler.askNotificationPermission()
            },
            onShare = { context.share(it) }
        )
    }
}