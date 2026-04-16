package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.contactSupport
import app.paralino.core.MR
import app.paralino.core.presentation.settings.general.GeneralInfoViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.GeneralInfoScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun GeneralInfoScreen(
    appState: MainAppState,
    vm: GeneralInfoViewModel = koinViewModel()
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding().fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_section_general_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        val context = LocalContext.current
        _root_ide_package_.app.paralino.core.ui.settings.GeneralInfoScreenCore(
            appState = appState,
            vm = vm,
            paddingValues = it,
            onLicencesClick = {
                appState.navController.navigate(AppScreen.Settings.Licences.routeSignature)
            },
            onShowOnboarding = {
                appState.navController.navigate(AppScreen.Settings.WelcomeOnboarding.routeSignature)
            },
            onContactSupport = {
                context.contactSupport()
            }
        )
    }
}