package app.paralino.android.ui.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenEvent
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenState
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.BottomSheetsScreen
import app.paralino.android.ui.settings.SettingsBigHeader
import app.paralino.android.ui.settings.editprofile.EditProfileScreen
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.shared.getFormValidationErrorText
import app.paralino.android.utils.extensions.safeCast
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun ProfileSetupScreen(
    appState: MainAppState,
    vm: EditProfileViewModel = getViewModel(),
) {

    val uiState by vm.uiState.collectAsState(initial = EditProfileScreenState())

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is EditProfileScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                is EditProfileScreenEvent.ValidationError -> {
                    val errorText = getFormValidationErrorText(
                        context = context,
                        validation = event.validationResult.type
                    )
                    appState.showSnackbar(errorText)
                }
                EditProfileScreenEvent.ProfileUpdated -> {
//                    appState.navController.popBackStack()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    Column() {
        SettingsBigHeader(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
            name = stringResource(id = R.string.welcome_account_edit_profile)
        )
        EditProfileScreen(
            state = uiState,
            onImageClick = {
                appState.uiScope.launch {
                    appState.showBottomSheetForResult(BottomSheetsScreen.EmojiPicker)
                        .safeCast<ImageModel>()?.let {
                            vm.onImageChange(it)
                        }
                }
            },
            onNameChange = { vm.onNameChange(it) },
            onDeviceNameChange = { vm.onDeviceNameChange(it) },
            onCreateGroupClick = { vm.proceed() }
        )
    }

}