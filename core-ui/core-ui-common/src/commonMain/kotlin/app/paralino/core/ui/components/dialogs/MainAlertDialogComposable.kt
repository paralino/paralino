package app.paralino.core.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import app.paralino.core.MR
import app.paralino.core.presentation.common.AlertDialogButtonData
import app.paralino.core.presentation.common.AlertDialogData
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
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
        BoxWithConstraints {
            val maxHeight = this.maxHeight
            IosKeyboardSafeDialog {
                AlertDialog(
                    properties = DialogProperties(
                        dismissOnBackPress = canDismiss,
                        dismissOnClickOutside = canDismiss
                    ),
                    onDismissRequest = { onAction(null) },
                    title = {
                        Text(
                            text = title,
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
                        // ignore for now since lineHeight is not scaled so we don't gain much
//                    BasicText(
//                        modifier = Modifier.fillMaxWidth(),
//                        text = message.plus(message).annotatedBoldCompose(),
//                        style = LocalTextStyle.current.copy(
//                            lineHeight = 18.sp,
//                            textAlign = TextAlign.Center,
//                            color = LocalTextStyle.current.color.copy(alpha = LocalContentAlpha.current)
//                        ),
//                        autoSize = TextAutoSize.StepBased(
//                            minFontSize = 10.sp,
//                            maxFontSize = 14.sp
//                        )
//                    )
                    },
                    buttons = {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(bottom = 10.dp)
                                .heightIn(max = maxHeight * 0.45f)
                                .verticalScroll(rememberScrollState())
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
    var shouldShowLongTapInfo by remember(dialogId) { mutableStateOf(false) }

    if (buttonData.needsLongConfirmation) {
        val isPressed by interactionSource.collectIsPressedAsState()
        var wasPressed by remember(dialogId) { mutableStateOf(false) }

        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
                shouldShowLongTapInfo = true
            }
        }

        LaunchedEffect(isPressed) {
            if (isPressed && !wasPressed) {
                // start timer
                wasPressed = true
                clickAnimation = true
                shouldShowLongTapInfo = true
                delay(1050)
                onAction(buttonData.id)
                clickAnimation = false
            } else if (wasPressed && !isPressed) {
                // check timer, reset
                wasPressed = false
                clickAnimation = false
                shouldShowLongTapInfo = true
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(buttonData.text)
                AnimatedVisibility(shouldShowLongTapInfo) {
                    Text(
                        text = stringResource(MR.strings.button_hold_hint),
                        style = MaterialTheme.typography.overline.copy(
                            color = LocalContentColor.current.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
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
