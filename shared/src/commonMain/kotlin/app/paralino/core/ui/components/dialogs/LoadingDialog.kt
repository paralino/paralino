package app.paralino.core.ui.components.dialogs

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBold

@Composable
fun LoadingDialog(
    text: String,
    @FloatRange(0.0, 1.0) progress: Float? = null
) {
    AppTheme {
        AlertDialog(
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            onDismissRequest = {},
            title = {
                Text(
                    text = text.annotatedBold(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (progress != null) {
                        CircularProgressIndicator(progress = progress)
                    } else {
                        CircularProgressIndicator()
                    }
                }
            },
            buttons = {}
        )
    }
}