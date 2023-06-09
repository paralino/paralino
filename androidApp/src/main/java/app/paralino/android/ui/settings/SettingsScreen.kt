package app.paralino.android.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.navigation.Deeplink
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.AppIcons.asPainterResource
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.android.utils.extensions.openUri
import app.paralino.android.utils.extensions.share
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.utils.Emojis
import org.koin.androidx.compose.getViewModel

@Composable
fun SettingsScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {

    val accountState by viewModel.accountState.collectAsState(Payload.Loading)

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        SettingsBigHeader(
            name = stringResource(id = R.string.title_settings),
            bottomPadding = if (accountState.data?.userState?.isGuestUser == true) 0.dp
            else dimensionResource(id = R.dimen.activity_side_margin_medium_large)
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
                            appState.navController.navigate(
                                AppScreen.Settings.ProfileDetails.ConvertAccount.routeSignature
                            )
                        }
                    },
                    onErrorItemClick = { }
                )

                SettingsActionItem(
                    name = stringResource(id = R.string.settings_section_account_title),
                    icon = AppIcons.AccountCircle
                ) {
                    appState.navController.navigate(AppScreen.Settings.ProfileDetails.routeSignature)
                }

                SettingsActionItem(
                    name = stringResource(id = R.string.settings_section_personal_code_title),
                    icon = AppIcons.Key
                ) {
                    appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
                }

                SettingsActionItem(
                    name = stringResource(id = R.string.blocklist_screen_title),
                    icon = AppIcons.Block
                ) {
                    appState.navController.navigate(AppScreen.Settings.BlockList.routeSignature)
                }

//                SettingsActionItem(
//                    name = stringResource(id = R.string.settings_section_general_title),
//                    icon = AppIcons.Share
//                ) {
//                    appState.navController.navigate(AppScreen.SettingsRoot.PersonalCodes.routeSignature)
//                }

            }
            Payload.Loading -> {}
            is Payload.Error -> {}
        }

        SettingSectionInfo(
            onLicencesClick = {
                appState.navController.navigate(AppScreen.Settings.Licences.routeSignature)
            }
        )
    }
}

@Composable
fun SettingSectionInfo(
    onLicencesClick: () -> Unit
) {
    val context = LocalContext.current
    Column {
        SettingsSectionHeader(name = stringResource(id = R.string.settings_section_general_title))
//    SettingsActionItem(
//        name = stringResource(id = R.string.setting_title_rate),
//        icon = AppIcons.Celebration
//    ) {}
        SettingsActionItem(
            name = stringResource(id = R.string.setting_title_share),
            icon = AppIcons.Share
        ) {
            context.share(Deeplink.homeUrl)
        }
//    SettingsActionItem(
//        name = stringResource(id = R.string.setting_title_help),
//        icon = AppIcons.Help
//    ) {}
        SettingsActionItem(
            name = stringResource(id = R.string.setting_title_sourceCode),
            icon = AppIcons.Code
        ) {
            context.openUri(Deeplink.sourceCodeUrl)
        }
        SettingsActionItem(
            name = stringResource(id = R.string.setting_title_privacy),
            icon = AppIcons.PrivacyTip
        ) {
            context.openUri(Deeplink.privacyPolicyUrl)
        }
        SettingsActionItem(
            name = stringResource(id = R.string.setting_title_libraries),
            icon = AppIcons.LibraryBooks
        ) {
            onLicencesClick()
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = "v${app.paralino.android.BuildConfig.VERSION_NAME} (${app.paralino.android.BuildConfig.VERSION_CODE})",
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
fun SettingsBigHeader(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = dimensionResource(id = R.dimen.activity_side_margin_medium_large),
    name: String
) {
    Text(
        text = name.annotatedBold(),
        style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .padding(
                top = dimensionResource(id = R.dimen.activity_side_margin_medium_large),
                bottom = bottomPadding
            )
    )
}

@Composable
fun SettingsSectionHeader(
    modifier: Modifier = Modifier,
    name: String
) {
    Text(
        text = name.annotatedBold(), //.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Light),
        modifier = modifier
            .padding(
                top = dimensionResource(id = R.dimen.activity_side_margin_medium_large),
                bottom = dimensionResource(id = R.dimen.activity_side_margin_small)
            )
            .alpha(ContentAlpha.medium)
    )
}

@Composable
fun SettingsActionItem(
    modifier: Modifier = Modifier,
    name: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colors.surface,
    showEndArrow: Boolean = true,
    onClick: () -> Unit
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        val (iconMain, text, iconArrow) = createRefs()

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .constrainAs(iconMain) {
                    this.start.linkTo(parent.start)
                    this.top.linkTo(parent.top)
                    this.bottom.linkTo(parent.bottom)
                }
                .size(46.dp)
                .background(
                    color = iconTint,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        )
        Text(
            modifier = Modifier
                .constrainAs(text) {
                    this.linkTo(
                        start = iconMain.end,
                        end = iconArrow.start,
                        top = parent.top,
                        bottom = parent.bottom,
                        horizontalBias = 0f
                    )
                    width = Dimension.preferredWrapContent
                }
                .padding(horizontal = 12.dp),
            text = name,
            style = MaterialTheme.typography.subtitle2.copy(fontSize = 18.sp)
        )
        Icon(
            modifier = Modifier
                .constrainAs(iconArrow) {
                    this.end.linkTo(parent.end)
                    this.top.linkTo(parent.top)
                    this.bottom.linkTo(parent.bottom)
                    visibility = if (showEndArrow) Visibility.Visible else Visibility.Gone
                }
                .width(20.dp),
            painter = AppIcons.ChevronRight.asPainterResource(),
            tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
            contentDescription = null
        )
    }
}

@Preview(widthDp = 320, heightDp = 600)
@Preview(widthDp = 320, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainPreview() {
    AppTheme {
        Surface {
            SettingSectionInfo(onLicencesClick = {})
        }
    }
}