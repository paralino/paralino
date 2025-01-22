package app.paralino.core.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.presentation.settings.general.GeneralInfoScreenStateUiEvent
import app.paralino.core.presentation.settings.general.GeneralInfoViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun GeneralInfoScreenCore(
    appState: CoreUiState,
    vm: GeneralInfoViewModel,
    paddingValues: PaddingValues,
    onLicencesClick: () -> Unit,
    onShowOnboarding: () -> Unit,
    onContactSupport: () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(paddingValues)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 60.dp)
    ) {
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_help),
            icon = CoreAppIcons.LibraryBooks.asPainterResource(),
            type = SettingsActionItemType.Link
        ) {
            uriHandler.openUri(BackendResourceIDs.helpSupportUrl)
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_contactSupport),
            icon = CoreAppIcons.Email.asPainterResource(),
            tag = BackendResourceIDs.supportEmail
        ) {
            onContactSupport()
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_onboarding),
            icon = CoreAppIcons.Replay.asPainterResource(),
            onLongClick = {
                vm.onEvent(GeneralInfoScreenStateUiEvent.ResetAccountWelcomeOnboarding)
            }
        ) {
            onShowOnboarding()
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_privacy),
            icon = CoreAppIcons.PrivacyTip.asPainterResource(),
            type = SettingsActionItemType.Link
        ) {
            uriHandler.openUri(BackendResourceIDs.privacyPolicyUrl)
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_terms),
            icon = CoreAppIcons.PrivacyTip.asPainterResource(),
            type = SettingsActionItemType.Link
        ) {
            uriHandler.openUri(BackendResourceIDs.termsOfUseUrl)
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_libraries),
            icon = CoreAppIcons.LibraryBooks.asPainterResource()
        ) {
            onLicencesClick()
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_errorReporting),
            description = stringResource(MR.strings.setting_description_errorReporting),
            icon = CoreAppIcons.Code.asPainterResource(),
            type = SettingsActionItemType.Switch(uiState.isErrorReportingEnabled),
            onClick = { vm.onEvent(GeneralInfoScreenStateUiEvent.ToggleCrashReporting) }
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = uiState.appVersion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}