package app.paralino.android.ui.login

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.core.presentation.login.LoginScreenUiType
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun LoggedOutScreen(
    appState: MainAppState,
) {

    val systemUiController = rememberSystemUiController()
    val defaultColor = MaterialTheme.colors.background
    val darkIconsDefault = !isSystemInDarkTheme()

    DisposableEffect(systemUiController) {
        systemUiController.setSystemBarsColor(
            color = AppColors.darkBackground,
            darkIcons = false
        )
        onDispose {
            systemUiController.setSystemBarsColor(
                color = defaultColor,
                darkIcons = darkIconsDefault
            )
        }
    }

    AppTheme(useDarkTheme = true) {
        Surface(color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom
            ) {

                Spacer(modifier = Modifier.size(10.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .background(color = MaterialTheme.colors.primary, shape = CircleShape)
                        .size(150.dp)
                        .scale(1.3f)
                        .align(alignment = Alignment.CenterHorizontally)
                )

                Spacer(Modifier.weight(1f))

                val infoTextStyle = MaterialTheme.typography.h4.copy(
                    fontWeight = FontWeight.Thin,
                    lineHeight = 40.sp
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
                    onClick = { appState.navController.navigate(AppScreen.LoggedOut.Login.routeSignature) }
                ) {
                    Icon(imageVector = Icons.Rounded.Login, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.welcome_screen_button_join))
                }
                Spacer(Modifier.size(10.dp))

                Text(
                    text = stringResource(id = R.string.login_signup_switch_button_login).annotatedBold(),
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            appState.navController.navigate(
                                AppScreen.LoggedOut.Login.route(LoginScreenUiType.LOGIN)
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(Modifier.size(20.dp))
            }
        }
    }
}