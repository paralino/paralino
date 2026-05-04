package app.paralino.core.ui.settings.billing.licence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.billing.licence.LicenceCodeEvent
import app.paralino.core.presentation.settings.billing.licence.LicenceCodeUiEvent
import app.paralino.core.presentation.settings.billing.licence.LicenceCodeViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LicenceCodeScreenCore(
    appState: CoreUiState,
    vm: LicenceCodeViewModel,
    onNavigateBack: () -> Unit
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                LicenceCodeEvent.OnNavigateBack, LicenceCodeEvent.OnSuccess -> {
                    onNavigateBack()
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current

    ActionScaffold(
        modifier = Modifier.fillMaxSize(),
        buttonState = AppButtonState.loadingState(uiState.isLoading),
        buttonText = stringResource(MR.strings.licence_button_activate),
        buttonIcon = null,
        onConfirm = {
            focusManager.clearFocus()
            vm.onEvent(LicenceCodeUiEvent.OnSubmit)
        },
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NoticeInfoChip(
                modifier = Modifier.fillMaxWidth(),
                icon = CoreAppIcons.LicenseTicket.asPainterResource(),
                text = stringResource(MR.strings.licence_screen_info)
            )

            Spacer(modifier = Modifier.weight(1f))

            MainInputField(
                text = uiState.code,
                textStyle = MaterialTheme.typography.subtitle2,
                hint = stringResource(resource = MR.strings.licence_code_hint),
                hasError = uiState.isErrorStep,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Characters,
                focusManager = focusManager,
                onInputChange = { vm.onEvent(LicenceCodeUiEvent.OnCodeChange(it)) }
            )

            var errorText by remember { mutableStateOf(uiState.stepText) }
            LaunchedEffect(uiState.stepText) {
                if (!uiState.stepText.isNullOrBlank()) {
                    errorText = uiState.stepText ?: ""
                }
            }

            AnimatedVisibility(
                visible = !uiState.stepText.isNullOrBlank(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = errorText ?: "",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .animateContentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            color = if (uiState.isErrorStep) {
                                AppColors.errorBackground
                            } else {
                                AppColors.accent.copy(alpha = 0.1f)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
