package app.paralino.android.ui.login

import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.login.ProfileSetupScreenCore
import app.paralino.core.utils.extensions.safeCast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun ProfileSetupScreen(
    appState: MainAppState,
    vm: EditProfileViewModel = getViewModel(),
) {
    ProfileSetupScreenCore(
        appState = appState,
        vm = vm,
        onBottomSheetAction = {
            appState.uiScope.launch {
                appState.showBottomSheetForResult(it).safeCast<ImageModel>()?.let {
                    vm.onImageChange(it)
                }
            }
        }
    )
}