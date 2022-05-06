package com.zbapp.locationtrack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.theme.AppTheme

@Composable
fun MainAlertDialog(
    title: String,
    message: String,
    buttons: List<AlertDialogButtonData> = listOf(
        AlertDialogButtonData(
            "ok",
            stringResource(id = R.string.button_ok)
        )
    ),
    onAction: (buttonId: String?) -> Unit
) {
    AppTheme {
        AlertDialog(
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
                    message,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            buttons = {
                Column(modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)) {
                    buttons.forEach { buttonData ->
                        TextButton(
                            onClick = { onAction(buttonData.id) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(buttonData.text)
                        }
                    }
                }
            }
        )
    }
}

data class AlertDialogButtonData(
    val id: String,
    val text: String
)