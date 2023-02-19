package app.paralino.android.ui.settings.editprofile

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenEvent
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenState
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.*
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.shared.getFormValidationErrorText
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.android.utils.extensions.safeCast
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun EditProfileScreen(
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
                    appState.navController.popBackStack()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = R.string.settings_account_edit_profile)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        EditProfileScreen(
            state = uiState,
            onImageClick = {
                appState.uiScope.launch {
                    appState.showBottomSheetForResult(BottomSheetsScreen.EmojiPicker(Emojis.userEmojis))
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

@Composable
fun EditProfileScreen(
    state: EditProfileScreenState,
    onImageClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.fillMaxHeight(1f))

        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            ImageModelImage(
                imageModel = state.image,
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        onImageClick()
                    }
            )
        }

        Spacer(modifier = Modifier.size(20.dp))

        MainInputField(
            text = state.name,
            hint = stringResource(id = R.string.settings_profile_name_placeholder),
            hasError = state.hasNameError,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Words,
            focusManager = focusManager,
            onInputChange = onNameChange
        )

        Spacer(modifier = Modifier.size(20.dp))

        MainInputField(
            text = state.deviceName,
            hint = stringResource(id = R.string.groups_sharing_device_name_placeholder),
            hasError = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Words,
            focusManager = focusManager,
            onInputChange = onDeviceNameChange
        )

        Spacer(modifier = Modifier.size(80.dp))

        FilledButton(
            style = ButtonStyle.BIG,
            state = if (state.actionLoading == null)
                AppButtonState.CONTENT
            else
                AppButtonState.loadingStateWithCheck(state.actionLoading!!),
            icon = AppIcons.Save,
            onClick = {
                focusManager.clearFocus()
                onCreateGroupClick()
            }
        ) {
            Text(text = stringResource(id = R.string.button_save))
        }

        Spacer(modifier = Modifier.fillMaxHeight(1f))
    }
}