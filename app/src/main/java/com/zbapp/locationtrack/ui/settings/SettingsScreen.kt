package com.zbapp.locationtrack.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.AppScreen
import com.zbapp.locationtrack.ui.theme.AppTheme
import java.util.*

@Composable
fun SettingsScreen(navController: NavController) {
//    Surface {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        SettingSectionAccount(navController)
        SettingSectionLocation()
        SettingSectionInfo()
    }
//    }
}

@Composable
fun SettingSectionAccount(navController: NavController) {
    SettingsSectionHeader(name = stringResource(id = R.string.settings_section_account_title))
    SettingsActionItem(
        name = stringResource(id = R.string.settings_section_account_title),
        icon = Icons.Rounded.AccountCircle
    ) { navController.navigate(AppScreen.SettingsRoot.ProfileDetails.routeSignature) }
}

@Composable
fun SettingSectionLocation() {
    SettingsSectionHeader(name = stringResource(id = R.string.settings_section_location_title))
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_location_defaults),
        icon = Icons.Rounded.EditLocation
    ) {}
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_location_sharing),
        icon = Icons.Rounded.PersonPinCircle
    ) {}
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_location_history),
        icon = Icons.Rounded.History
    ) {}
}

@Composable
fun SettingSectionInfo() {
    SettingsSectionHeader(name = stringResource(id = R.string.settings_section_general_title))
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_rate),
        icon = Icons.Rounded.Celebration
    ) {}
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_share),
        icon = Icons.Rounded.Share
    ) {}
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_help),
        icon = Icons.Rounded.Help
    ) {}
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_sourceCode),
        icon = Icons.Rounded.Code
    ) {}
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_privacy),
        icon = Icons.Rounded.PrivacyTip
    ) {}
    SettingsActionItem(
        name = stringResource(id = R.string.setting_title_libraries),
        icon = Icons.Rounded.LibraryBooks
    ) {}

    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            style = MaterialTheme.typography.caption,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SettingsSectionHeader(name: String) {
    Text(
        text = name.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.h6,
        modifier = Modifier
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
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.button,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun LoggedOutSettingsSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login to share your location")
        Button(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            onClick = { }
        ) {
            Icon(imageVector = Icons.Rounded.Login, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Login")
        }
        Spacer(Modifier.size(20.dp))
        Text("Have public tracking code?")
        OutlinedTextField(
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            value = TextFieldValue("HJKI-LJZU"),
            placeholder = {
                Text(
                    "hint",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onValueChange = {},
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Preview(widthDp = 320, heightDp = 600)
@Preview(widthDp = 320, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainPreview() {
    AppTheme {
        SettingsScreen(rememberNavController())
    }
}