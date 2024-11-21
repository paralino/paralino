package app.paralino.core.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBold

@Composable
fun LoadingDialog(
    text: String?,
    progress: Float? = null
) {
    val title: (@Composable () -> Unit)? = if (text != null) {
        @Composable {
            Text(
                text = text.annotatedBold(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        null
    }

    AppTheme {
        AlertDialog(
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            onDismissRequest = {},
            title = title,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (progress != null) {
                        CircularProgressIndicator(progress = progress, strokeCap = StrokeCap.Round)
                    } else {
                        LinearProgressIndicator(strokeCap = StrokeCap.Round)
                    }
                }
            },
            buttons = {}
        )
    }
}