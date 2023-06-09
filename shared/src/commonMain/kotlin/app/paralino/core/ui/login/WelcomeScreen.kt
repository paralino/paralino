package app.paralino.core.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.ExperimentalResourceApi


@OptIn(ExperimentalResourceApi::class)
@Composable
fun WelcomeScreen(
    hasLogin: Boolean,
    onLoginClick: () -> Unit,
    onStartClick: () -> Unit,
) {

    AppTheme(useDarkTheme = true) {
        Surface(color = MaterialTheme.colors.background) {

            Image(
                painter = painterResource(MR.images.paralino_start_background),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                Spacer(modifier = Modifier.size(40.dp))

                Text(stringResource(MR.strings.app_name), style = MaterialTheme.typography.h2)
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(MR.strings.welcome_screen_info_1),
                    style = MaterialTheme.typography.h6.copy(
                        fontWeight = FontWeight.SemiBold,
                    )
                )

                Spacer(Modifier.weight(1f))

                Image(
                    painter = org.jetbrains.compose.resources.painterResource("drawable/ic_launcher_foreground.xml"),
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
                    Icon(org.jetbrains.compose.resources.painterResource("drawable/ic_login.xml"), contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(MR.strings.welcome_screen_button_join))
                }
                Spacer(Modifier.size(10.dp))

                if (hasLogin) {
                    Text(
                        text = stringResource(MR.strings.login_signup_switch_button_login).annotatedBold(),
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