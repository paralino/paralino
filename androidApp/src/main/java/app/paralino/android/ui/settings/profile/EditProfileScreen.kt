package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.utils.extensions.safeCast
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun EditProfileScreen(
    appState: MainAppState,
    vm: EditProfileViewModel = getViewModel()
) {
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_account_edit_profile),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        app.paralino.core.ui.settings.profile.EditProfileScreen(
            appState = appState,
            vm = vm,
            onBottomSheetAction = {
                appState.uiScope.launch {
                    appState.showBottomSheetForResult(it).safeCast<ImageModel>()?.let {
                        vm.onImageChange(it)
                    }
                }
            },
            onExit = { appState.navController.popBackStack() }
        )
    }
}