package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.theme.ButtonStyle

@Composable
fun ActionScaffold(
    modifier: Modifier = Modifier,
    buttonStyle: ButtonStyle = ButtonStyle.BIG(iconSize = 24.dp),
    buttonState: AppButtonState = AppButtonState.CONTENT,
    buttonIcon: Painter?,
    buttonText: String,
    onConfirm: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            ScaffoldBottomBarButton(
                modifier = Modifier,
                buttonStyle = buttonStyle,
                buttonState = buttonState,
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { }
            )
            .padding(bottom = 20.dp, top = 8.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun ScaffoldBottomBarButton(
    modifier: Modifier = Modifier,
    buttonStyle: ButtonStyle = ButtonStyle.BIG,
    buttonState: AppButtonState = AppButtonState.CONTENT,
    buttonIcon: Painter?,
    buttonText: String,
    onClick: () -> Unit
) {
    ScaffoldBottomBarButtons(modifier = modifier) {
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