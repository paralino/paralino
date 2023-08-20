package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import app.paralino.android.ui.MainAppState
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.ui.settings.profile.ChangePasswordScreenCore
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangePasswordScreen(
    appState: MainAppState,
    vm: ChangePasswordViewModel = getViewModel(),
) {
    ChangePasswordScreenCore(
        appState = appState,
        vm = vm,
        onNavigateBack = { appState.navController.popBackStack() }
    )
}
