package app.paralino.mobile.ui.login

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
import app.paralino.mobile.R
import app.paralino.mobile.ui.MainAppState
import app.paralino.mobile.ui.components.BottomSheetsScreen
import app.paralino.mobile.ui.settings.SettingsBigHeader
import app.paralino.mobile.ui.settings.editprofile.EditProfileScreen
import app.paralino.mobile.ui.settings.editprofile.EditProfileViewModel
import app.paralino.mobile.ui.shared.errorString
import app.paralino.mobile.ui.shared.getFormValidationErrorText
import app.paralino.mobile.utils.extensions.safeCast
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
            onCreateGroupClick = { vm.proceed() }
        )
    }

}