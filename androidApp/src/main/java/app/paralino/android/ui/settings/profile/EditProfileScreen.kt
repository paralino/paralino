package app.paralino.android.ui.settings.profile

import androidx.compose.runtime.Composable
import app.paralino.android.ui.MainAppState
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.utils.extensions.safeCast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun EditProfileScreen(
    appState: MainAppState,
    vm: EditProfileViewModel = getViewModel()
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