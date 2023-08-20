package app.paralino.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.SettingsBigHeader
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.settings.profile.AccountInfoHeader
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun SettingsScreenCore(
    appState: CoreUiState,
    viewModel: SettingsViewModel,
    onShareApp: (String) -> Unit,
    openConvertAccount: () -> Unit,
    openProfileDetails: () -> Unit,
    openPersonalCodes: () -> Unit,
    openBlockList: () -> Unit,
    openLicences: () -> Unit,
) {

    val accountState by viewModel.accountState.collectAsState(Payload.Loading)

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        SettingsBigHeader(
            name = stringResource(MR.strings.title_settings),
            bottomPadding = if (accountState.data?.userState?.isGuestUser == true) 0.dp
            else 20.dp
        )

        when (val state = accountState) {
            is Payload.Success -> {
                AccountInfoHeader(
                    name = state.data.userState.name,
                    email = "",
                    userId = "",
                    image = state.data.userState.image ?: ImageModel.emoji(Emojis.robot),
                    isGuestUser = state.data.userState.isGuestUser,
                    errorItem = null,
                    onGuestAccountClick = {
                        if(state.data.canUseEmail) {
                            openConvertAccount()
                        }
                    },
                    onErrorItemClick = { },
                    onCopy = { viewModel.copyToClipboard(it) }
                )

                SettingsActionItem(
                    name = stringResource(MR.strings.settings_section_account_title),
                    icon = CoreAppIcons.AccountCircle.asPainterResource()
                ) {
                    openProfileDetails()
                }

                SettingsActionItem(
                    name = stringResource(MR.strings.settings_section_personal_code_title),
                    icon = CoreAppIcons.Key.asPainterResource()
                ) {
                    openPersonalCodes()
                }

                SettingsActionItem(
                    name = stringResource(MR.strings.blocklist_screen_title),
                    icon = CoreAppIcons.Block.asPainterResource()
                ) {
                    openBlockList()
                }
            }
            Payload.Loading -> {}
            is Payload.Error -> {}
        }

        SettingSectionInfo(
            appVersion = accountState.data?.versionName ?: "",
            onLicencesClick = { openLicences() },
            onShareApp = onShareApp
        )
    }
}

@Composable
fun SettingSectionInfo(
    appVersion: String,
    onLicencesClick: () -> Unit,
    onShareApp: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column {
        SettingsSectionHeader(name = stringResource(MR.strings.settings_section_general_title))

        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_share),
            icon = CoreAppIcons.Share.asPainterResource()
        ) {
            onShareApp(BackendResourceIDs.homeUrl)
        }
//    SettingsActionItem(
//        name = stringResource(MR.strings.setting_title_help),
//        icon = AppIcons.Help
//    ) {}
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_sourceCode),
            icon = CoreAppIcons.Code.asPainterResource()
        ) {
            uriHandler.openUri(BackendResourceIDs.sourceCodeUrl)
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_privacy),
            icon = CoreAppIcons.PrivacyTip.asPainterResource()
        ) {
            uriHandler.openUri(BackendResourceIDs.privacyPolicyUrl)
        }
        SettingsActionItem(
            name = stringResource(MR.strings.setting_title_libraries),
            icon = CoreAppIcons.LibraryBooks.asPainterResource()
        ) {
            onLicencesClick()
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = appVersion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsActionItem(
    modifier: Modifier = Modifier,
    name: String,
    icon: Painter,
    iconTint: Color = MaterialTheme.colors.surface,
    showEndArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .background(
                    color = iconTint,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            text = name,
            style = MaterialTheme.typography.subtitle2.copy(fontSize = 18.sp),
            textAlign = TextAlign.Start
        )
        if(showEndArrow) {
            Icon(
                modifier = Modifier.width(20.dp),
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                contentDescription = null
            )
        }
    }
}