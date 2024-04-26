package app.paralino.core.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBold

@Composable
fun MainAlertDialog(
    data: AlertDialogData,
    onAction: (buttonId: String?) -> Unit,
) {
    MainAlertDialog(
        title = data.title,
        message = data.message,
        buttons = data.buttons,
        canDismiss = data.canDismiss,
        onAction = onAction
    )
}

@Composable
fun MainAlertDialog(
    title: String,
    message: String,
    canDismiss: Boolean = true,
    buttons: List<AlertDialogButtonData>,
    onAction: (buttonId: String?) -> Unit,
) {
    AppTheme {
        AlertDialog(
            properties = DialogProperties(
                dismissOnBackPress = canDismiss,
                dismissOnClickOutside = canDismiss
            ),
            onDismissRequest = { onAction(null) },
            title = {
                Text(
                    title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = message.annotatedBold(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            buttons = {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp)
                ) {
                    buttons.forEach { buttonData ->
                        TextButton(
                            onClick = { onAction(buttonData.id) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = contentColorFor(backgroundColor = buttonData.color),
                                backgroundColor = buttonData.color
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                        ) {
                            Text(buttonData.text)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MainAlertDialog.Show(onAction: (buttonId: String?) -> Unit) {
    MainAlertDialog(
        data = dialogData,
        onAction = onAction
    )
}
