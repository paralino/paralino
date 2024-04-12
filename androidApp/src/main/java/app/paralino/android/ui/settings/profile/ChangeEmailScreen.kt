package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.settings.profile.ChangeEmailScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ChangeEmailScreen(
    appState: MainAppState,
    vm: ChangeEmailViewModel = getViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(MR.strings.settings_account_edit_email)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = CoreAppIcons.Close.asPainterResource(),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        ChangeEmailScreenCore(
            appState = appState,
            vm = vm,
            onNavigateBack = { appState.navController.popBackStack() }
        )
    }
}

