package app.paralino.android.ui.components

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
import app.paralino.android.R
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.utils.extensions.annotatedBold

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
    onAction: (buttonId: String?) -> Unit,
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
    val text: String,
)

sealed interface MainAlertDialog {
    object Buttons {
        const val buttonOk = "ok"
        const val buttonCancel = "cancel"
    }

    val key: String


    object Logout : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.settings_account_logout_alert_title),
                message = stringResource(id = R.string.settings_account_logout_alert_message),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.button_ok)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    object DeleteAccount : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.settings_account_delete_alert_title),
                message = stringResource(id = R.string.settings_account_delete_alert_message),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.button_ok)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    object DeleteAccountConfirm : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = stringResource(id = R.string.settings_account_delete_alert_confirm_message),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.settings_account_delete_alert_cancel_button1)
                    ),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.settings_account_delete_alert_cancel_button2)
                    ),
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.settings_account_delete_alert_confirm_button)
                    ),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.settings_account_delete_alert_cancel_button3)
                    ),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.settings_account_delete_alert_cancel_button4)
                    ),
                ).shuffled(),
                onAction = onAction
            )
        }
    }

    object LeaveGroup : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.group_details_menu_leave),
                message = stringResource(id = R.string.group_details_leave_group_alert_title),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.group_details_leave_group_alert_button)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    object DeleteGroup : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.group_details_menu_delete),
                message = stringResource(id = R.string.group_details_delete_group_alert_title),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.group_details_delete_group_alert_button)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    data class DeleteGroupMember(
        val memberUserId: String,
        val memberName: String,
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = stringResource(id = R.string.group_members_remove_alert_title, memberName),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.group_details_remove_member_alert_button)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    object SharingBackgroundLocationMissing : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.groups_sharing_potential_error_title),
                message = stringResource(id = R.string.groups_sharing_potential_error_background_location_message),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.button_ok)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_skip)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    data class SharingErrors(val errorMessages: List<String>) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            val errorContent = errorMessages.joinToString(separator = "**\n- **", prefix = "- **", postfix = "**")
            MainAlertDialog(
                title = stringResource(id = R.string.groups_sharing_potential_error_title),
                message = stringResource(id = R.string.groups_sharing_potential_error_message, errorContent),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.groups_sharing_potential_error_button_enable)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    data class PersonalCodeDelete(
        val codeId: String
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = stringResource(id = R.string.invite_delete_alert_title),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_ok)
                    ),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    object SessionExpired : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_sessionExpired_title),
                message = stringResource(id = R.string.alert_sessionExpired_message),
                buttons = listOf(
                    AlertDialogButtonData(
                        id = Buttons.buttonOk,
                        text = stringResource(id = R.string.button_ok)
                    )
                ),
                onAction = onAction
            )
        }
    }

    object RecoveryResetAccount : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = stringResource(id = R.string.recover_account_alert_message_reset),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.button_confirm)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

    object RecoveryRecoverAccount : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = stringResource(id = R.string.recover_account_alert_message_recover),
                buttons = listOf(
                    AlertDialogButtonData(Buttons.buttonOk,
                        stringResource(id = R.string.button_confirm)),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_cancel)
                    ),
                ),
                onAction = onAction
            )
        }
    }

}