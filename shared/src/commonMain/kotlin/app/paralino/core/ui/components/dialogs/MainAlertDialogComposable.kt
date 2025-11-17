package app.paralino.core.ui.components.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBoldCompose
import kotlinx.coroutines.delay
import kotlin.math.pow

@Composable
fun MainAlertDialog(
    data: AlertDialogData,
    onAction: (buttonId: String?) -> Unit,
) {
    MainAlertDialog(
        dialogId = data.id,
        title = data.title,
        message = data.message,
        buttons = data.buttons,
        canDismiss = data.canDismiss,
        onAction = onAction
    )
}

@Composable
fun MainAlertDialog(
    dialogId: String,
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
                    text = message.annotatedBoldCompose(),
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
                        MainAlertDialogActionButton(
                            dialogId = dialogId,
                            buttonData = buttonData,
                            onAction = onAction
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun MainAlertDialogActionButton(
    modifier: Modifier = Modifier,
    dialogId: String,
    buttonData: AlertDialogButtonData,
    onAction: (buttonId: String?) -> Unit
) {
    val interactionSource = remember(dialogId) { MutableInteractionSource() }
    var clickAnimation by remember(dialogId) { mutableStateOf(false) }

    if (buttonData.needsLongConfirmation) {
        val isPressed by interactionSource.collectIsPressedAsState()
        var wasPressed by remember(dialogId) { mutableStateOf(false) }

        LaunchedEffect(isPressed) {
            if (isPressed && !wasPressed) {
                // start timer
                wasPressed = true
                clickAnimation = true
                delay(1050)
                onAction(buttonData.id)
                clickAnimation = false
            } else if (wasPressed && !isPressed) {
                // check timer, reset
                wasPressed = false
                clickAnimation = false
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(
                    animateFloatAsState(
                        targetValue = if (clickAnimation) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = { fraction ->
                                fraction.pow(0.8f)
                            }
                        )
                    ).value
                )
                .background(
                    color = AppColors.redMain.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        TextButton(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            interactionSource = interactionSource,
            onClick = {
                if (!buttonData.needsLongConfirmation) {
                    onAction(buttonData.id)
                }
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = contentColorFor(backgroundColor = buttonData.color),
                backgroundColor = buttonData.color
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(buttonData.text)
        }
    }
}

@Composable
private fun rememberDelayedClickState(
    delayMillis: Long
): MutableState<Boolean> {
    val trigger = remember { mutableStateOf(false) }
    LaunchedEffect(delayMillis) {
        delay(delayMillis)
        trigger.value = true
    }
    return trigger
}

@Composable
fun MainAlertDialog.Show(onAction: (buttonId: String?) -> Unit) {
    MainAlertDialog(
        data = dialogData,
        onAction = onAction
    )
}
