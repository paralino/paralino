package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ActionScaffold(
    modifier: Modifier = Modifier,
    additionalButtonBottomPadding: Dp = 0.dp,
    buttonStyle: ButtonStyle = ButtonStyle.BIG(iconSize = 24.dp),
    buttonState: AppButtonState = AppButtonState.CONTENT,
    buttonBackground: Color = MaterialTheme.colors.background,
    buttonIcon: Painter?,
    buttonText: String,
    onConfirm: () -> Unit,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = {
            ScaffoldBottomBarButton(
                modifier = Modifier,
                paddingValues = PaddingValues(
                    top = 8.dp,
                    bottom = 20.dp + additionalButtonBottomPadding,
                    start = 20.dp,
                    end = 20.dp
                ),
                buttonStyle = buttonStyle,
                buttonState = buttonState,
                buttonBackground = buttonBackground,
                buttonIcon = buttonIcon,
                buttonText = buttonText,
                onClick = onConfirm
            )
        },
        content = content
    )
}

@Composable
fun ScaffoldBottomBarButtons(
    modifier: Modifier = Modifier,
    buttonBackground: Color = MaterialTheme.colors.background,
    paddingValues: PaddingValues = PaddingValues(
        top = 8.dp,
        bottom = 20.dp,
        start = 20.dp,
        end = 20.dp
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(buttonBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { }
            )
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun ScaffoldBottomBarButton(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(
        top = 8.dp,
        bottom = 20.dp,
        start = 20.dp,
        end = 20.dp
    ),
    buttonStyle: ButtonStyle = ButtonStyle.BIG,
    buttonState: AppButtonState = AppButtonState.CONTENT,
    buttonBackground: Color = MaterialTheme.colors.background,
    buttonIcon: Painter?,
    buttonText: String,
    onClick: () -> Unit
) {
    ScaffoldBottomBarButtons(
        modifier = modifier,
        buttonBackground = buttonBackground,
        paddingValues = paddingValues
    ) {
        FilledButton(
            modifier = Modifier,
            style = buttonStyle,
            state = buttonState,
            icon = buttonIcon,
            onClick = onClick
        ) {
            Text(text = buttonText)
        }
    }
}

@Composable
fun OnboardingSkipButton(
    modifier: Modifier = Modifier,
    skipButtonText: String = stringResource(MR.strings.button_skip),
    onSkip: () -> Unit
) {
    FilledButton(
        modifier = modifier.padding(top = 6.dp),
        style = remember { ButtonStyle.BIG(paddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) },
        state = AppButtonState.CONTENT,
        icon = null,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = MaterialTheme.colors.onBackground
        ),
        elevation = null,
        onClick = onSkip
    ) {
        Text(text = skipButtonText)
    }
}