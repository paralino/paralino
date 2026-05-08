package app.paralino.core.ui.settings.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.paralino.core.MR
import app.paralino.core.models.base.SubscriptionOfferDialogContent
import app.paralino.core.provider.localizedString
import app.paralino.core.presentation.common.AlertDialogButtonData
import app.paralino.core.presentation.common.MainAlertDialog.Buttons
import app.paralino.core.ui.components.dialogs.MainAlertDialogActionButton
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.annotatedBoldCompose

@Composable
fun SubscriptionOfferDialogComposable(
    data: SubscriptionOfferDialogContent,
    onAction: (buttonId: String?) -> Unit,
) {
    val title = remember(data) {
        val language = try {
            Locale.current.language
        } catch (e: Exception) {
            "en"
        }
        data.i18nTitle?.get(language) ?: data.i18nTitle?.get("en")
    }
    val message = remember(data) {
        val language = try {
            Locale.current.language
        } catch (e: Exception) {
            "en"
        }
        data.i18nMessage?.get(language) ?: data.i18nMessage?.get("en")
    }

    SubscriptionOfferDialogComposable(
        dialogId = "offer",
        emoji = data.emoji,
        title = title,
        message = message,
        buttons = remember {
            listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = MR.strings.button_ok.localizedString(),
                    color = AlertDialogButtonData.positiveColor
                )
            )
        },
        canDismiss = true,
        onAction = onAction
    )
}

@Composable
fun SubscriptionOfferDialogComposable(
    dialogId: String,
    emoji: String?,
    title: String?,
    message: String?,
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
            title = if (title != null || emoji != null) {
                {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (title != null) {
                            Text(
                                text = title,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.h6
                            )
                        }

                        if (emoji != null) {
                            Text(
                                text = emoji,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.h2
                            )
                        }
                    }
                }
            } else {
                null
            },
            text = if (message != null) {
                {
                    Text(
                        text = message.annotatedBoldCompose(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                null
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
fun SubscriptionOfferDialogContent.Show(onAction: (buttonId: String?) -> Unit) {
    SubscriptionOfferDialogComposable(
        data = this,
        onAction = onAction
    )
}