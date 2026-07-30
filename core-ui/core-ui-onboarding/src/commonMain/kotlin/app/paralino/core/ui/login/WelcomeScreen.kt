package app.paralino.core.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.resources.icons.Res
import app.paralino.core.resources.icons.ic_launcher_background
import app.paralino.core.resources.icons.ic_launcher_foreground
import app.paralino.core.resources.icons.paralino_start_background
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.ui.theme.AppTypography
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun WelcomeScreen(
    vm: LoggedOutViewModel,
    paddingValues: PaddingValues,
    onLoginClick: () -> Unit,
    onStartClick: () -> Unit,
) {
    WelcomeScreen(
        paddingValues = paddingValues,
        onStartClick = {
            if (vm.didHaveWelcomeOnboarding()) {
                onLoginClick()
            } else {
                onStartClick()
            }
        }
    )
}

@Composable
fun WelcomeScreen(
    paddingValues: PaddingValues,
    onStartClick: () -> Unit,
) {
    AppTheme(useDarkTheme = true) {
        ActionScaffold(
            modifier = Modifier,
            additionalButtonBottomPadding = paddingValues.calculateBottomPadding(),
            buttonIcon = null,
            buttonText = stringResource(MR.strings.welcome_screen_button_join),
            buttonBackground = Color.Transparent,
            onConfirm = onStartClick
        ) {
            Image(
                painter = painterResource(resource = Res.drawable.paralino_start_background),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(it)
                    .padding(paddingValues)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                Spacer(modifier = Modifier.size(40.dp))

                Text(
                    text = stringResource(MR.strings.app_name),
                    style = AppTypography.Catamaran.h2.copy(
                        letterSpacing = 0.4.sp,
                    )
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .offset(y = -20.dp),
                    text = stringResource(MR.strings.welcome_screen_info_1),
                    style = MaterialTheme.typography.subtitle1.copy(
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium)
                    )
                )

                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colors.background,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .size(120.dp)
                        ,
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(resource = Res.drawable.ic_launcher_background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )

                    Image(
                        painter = painterResource(resource = Res.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.2f)
                    )
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}