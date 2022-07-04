package app.paralino.mobile.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.mobile.R
import app.paralino.mobile.ui.MainAppState
import app.paralino.mobile.ui.navigation.AppScreen

@Composable
fun LoggedOutScreen(
    appState: MainAppState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {

        Spacer(modifier = Modifier.size(30.dp))

        Icon(
            imageVector = Icons.Rounded.PrivacyTip,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .align(alignment = Alignment.CenterHorizontally)
        )

        Spacer(Modifier.weight(1f))

        val infoTextStyle = MaterialTheme.typography.h3.copy(
            fontWeight = FontWeight.Thin,
            lineHeight = 44.sp
        )
        Text(stringResource(R.string.welcome_screen_info_1), style = infoTextStyle)
        Text(stringResource(R.string.welcome_screen_info_2), style = infoTextStyle)
        Text(stringResource(R.string.welcome_screen_info_3), style = infoTextStyle)
        Text(stringResource(R.string.welcome_screen_info_4), style = infoTextStyle)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.welcome_screen_info_app),
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colors.surface)
                .padding(vertical = 4.dp, horizontal = 8.dp)
        )

        Spacer(Modifier.weight(1f))
        Button(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            onClick = { appState.navController.navigate(AppScreen.AppStartRoot.Login.routeSignature) }
        ) {
            Icon(imageVector = Icons.Rounded.Login, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.welcome_screen_button_join))
        }
        Spacer(Modifier.size(20.dp))
    }
}