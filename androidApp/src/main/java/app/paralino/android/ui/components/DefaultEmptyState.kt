package app.paralino.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold

@Composable
fun DefaultEmptyState(
    modifier: Modifier = Modifier,
    content: String,
    icon: ImageVector? = null,
    buttonText: String? = null,
    buttonIcon: ImageVector? = null,
    buttonStyle: ButtonStyle = ButtonStyle.BIG,
    buttonState: AppButtonState = AppButtonState.CONTENT,
    onClick: (() -> Unit)? = null,
) {
    DefaultEmptyState(
        modifier = modifier,
        content = content,
        icon = icon,
        buttons = {
            if (buttonText != null) {
                FilledButton(
                    style = buttonStyle,
                    state = buttonState,
                    icon = buttonIcon,
                    onClick = onClick ?: { }
                ) {
                    Text(buttonText)
                }
            }
        }
    )
}

@Composable
fun DefaultEmptyState(
    modifier: Modifier = Modifier,
    content: String,
    icon: ImageVector? = null,
    buttons: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Spacer(modifier = Modifier.size(20.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .alpha(ContentAlpha.disabled)
            )
        }

        Spacer(modifier = Modifier.size(50.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = content.annotatedBold(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.size(50.dp))

        buttons()
    }
}