package app.paralino.android.ui.login

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.core.presentation.login.LoggedOutScreenState
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.androidx.compose.getViewModel

@Composable
fun LoggedOutScreen(
    appState: MainAppState,
    vm: LoggedOutViewModel = getViewModel()
) {
    val state by vm.uiState.collectAsState(LoggedOutScreenState())
    LoggedOutScreen(
        hasLogin = state.hasLogin,
        onLoginClick = {
            appState.navController.navigate(AppScreen.LoggedOut.Login.routeSignature)
        },
        onStartClick = {
            appState.navController.navigate(
                AppScreen.LoggedOut.Login.route(LoginScreenUiType.LOGIN)
            )
        }
    )
}

@Composable
fun LoggedOutScreen(
    hasLogin: Boolean,
    onLoginClick: () -> Unit,
    onStartClick: () -> Unit,
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

            MainBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                Spacer(modifier = Modifier.size(40.dp))

                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.h2)
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.welcome_screen_info_1),
                    style = MaterialTheme.typography.h6.copy(
                        fontWeight = FontWeight.Thin,
                    )
                )

                Spacer(Modifier.weight(1f))

                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .background(color = MaterialTheme.colors.primary, shape = CircleShape)
                        .border(
                            width = 8.dp,
                            color = MaterialTheme.colors.background,
                            shape = CircleShape
                        )
                        .size(120.dp)
                        .scale(1.2f)
                        .align(alignment = Alignment.CenterHorizontally)
                )

                Spacer(Modifier.weight(1f))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                    onClick = onLoginClick
                ) {
                    Icon(imageVector = AppIcons.Login, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.welcome_screen_button_join))
                }
                Spacer(Modifier.size(10.dp))

                if(hasLogin) {
                    Text(
                        text = stringResource(id = R.string.login_signup_switch_button_login).annotatedBold(),
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onStartClick()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun MainBackground() {
    val imageRes = R.drawable.image_main_background
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f }
            .drawWithContent {
                val colors = listOf(
                    0f to Color.Transparent,
                    0.05f to Color.Transparent,
                    0.35f to Color.Black,
                    0.65f to Color.Black,
                    0.95f to Color.Transparent,
                    1f to Color.Transparent,
                ).toTypedArray()
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(*colors),
                    blendMode = BlendMode.DstIn
                )
            },
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview
@Composable
fun LoggedOutScreenPreview() {
    AppTheme() {
        Surface {
            LoggedOutScreen(hasLogin = false, onLoginClick = {}, onStartClick = {})
        }
    }
}
