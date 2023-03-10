package app.paralino.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.paralino.android.R
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.core.models.user.BlocklistType

@Composable
fun MainAlertDialog(
    title: String,
    message: String,
    canDismiss: Boolean = true,
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
            properties = DialogProperties(dismissOnBackPress = canDismiss, dismissOnClickOutside = canDismiss),
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

@Stable
data class AlertDialogButtonData(
    val id: String,
    val text: String,
    val color: Color = Color.Transparent
) {
    companion object {
        val destructiveColor = AppColors.errorBackground
        val positiveColor = AppColors.accentLight.copy(alpha = 0.3f)
    }
}

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
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_ok),
                        AlertDialogButtonData.destructiveColor
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

    object DeleteAccount : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.settings_account_delete_alert_title),
                message = stringResource(id = R.string.settings_account_delete_alert_message),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_ok),
                        AlertDialogButtonData.destructiveColor
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
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.group_details_leave_group_alert_button),
                        AlertDialogButtonData.destructiveColor
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

    object DeleteGroup : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.group_details_menu_delete),
                message = stringResource(id = R.string.group_details_delete_group_alert_title),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.group_details_delete_group_alert_button),
                        AlertDialogButtonData.destructiveColor
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

    object BlockGroup : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.group_details_menu_block),
                message = stringResource(id = R.string.group_details_block_group_alert_title),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.group_details_block_group_alert_button),
                        AlertDialogButtonData.destructiveColor
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

    data class BlockUser(
        val userId: String
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.group_members_block_user),
                message = stringResource(id = R.string.group_members_block_user_alert_message),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.group_details_block_group_alert_button),
                        AlertDialogButtonData.destructiveColor
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

    data class UnblockGroupOrUser(
        val id: String,
        val type: BlocklistType
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = when(type){
                    BlocklistType.GROUP -> stringResource(id = R.string.blocklist_unblock_group_alert_title)
                    BlocklistType.USER -> stringResource(id = R.string.blocklist_unblock_user_alert_title)
                    BlocklistType.UNKNOWN -> stringResource(id = R.string.blocklist_unblock_alert_button)
                },
                message = when(type){
                    BlocklistType.GROUP -> stringResource(id = R.string.blocklist_unblock_group_alert_message)
                    BlocklistType.USER -> stringResource(id = R.string.blocklist_unblock_user_alert_message)
                    BlocklistType.UNKNOWN -> stringResource(id = R.string.blocklist_unblock_alert_unknown)
                },
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.blocklist_unblock_alert_button),
                        AlertDialogButtonData.positiveColor
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

    data class DeleteGroupMember(
        val memberUserId: String,
        val memberName: String,
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = stringResource(
                    id = R.string.group_members_remove_alert_title,
                    memberName
                ),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.group_details_remove_member_alert_button),
                        AlertDialogButtonData.destructiveColor
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

    data class DeleteMemberLocation(
        val memberId: String,
        val locationId: String,
        val isUserOwnDevice: Boolean,
        val memberName: String,
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            val message = if(isUserOwnDevice) {
                stringResource(
                    id = R.string.group_members_device_own_remove_alert_title
                )
            } else {
                stringResource(
                    id = R.string.group_members_device_other_remove_alert_title,
                    memberName
                )
            }
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = message,
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.group_details_remove_member_alert_button),
                        AlertDialogButtonData.destructiveColor
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

    object SharingBackgroundLocationMissing : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.groups_sharing_potential_error_title),
                message = stringResource(id = R.string.groups_sharing_potential_error_background_location_message),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_ok),
                        AlertDialogButtonData.positiveColor
                    ),
                    AlertDialogButtonData(
                        Buttons.buttonCancel,
                        stringResource(id = R.string.button_skip),
                        AlertDialogButtonData.destructiveColor
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
            val errorContent =
                errorMessages.joinToString(separator = "**\n- **", prefix = "- **", postfix = "**")
            MainAlertDialog(
                title = stringResource(id = R.string.groups_sharing_potential_error_title),
                message = stringResource(
                    id = R.string.groups_sharing_potential_error_message,
                    errorContent
                ),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_ok),
                        AlertDialogButtonData.positiveColor
                    )
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
                        stringResource(id = R.string.button_ok),
                        AlertDialogButtonData.destructiveColor
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
                        text = stringResource(id = R.string.button_ok),
                        AlertDialogButtonData.positiveColor
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
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_confirm),
                        AlertDialogButtonData.destructiveColor
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

    object RecoveryRecoverAccount : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.alert_title_confirm),
                message = stringResource(id = R.string.recover_account_alert_message_recover),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_confirm),
                        AlertDialogButtonData.positiveColor
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

    object GuestAccountCreating : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.login_signup_guest_alert_title),
                message = stringResource(id = R.string.login_signup_guest_alert_message),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_confirm),
                        AlertDialogButtonData.positiveColor
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

    object GuestAccountRecover : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.login_signup_guest_recover_alert_title),
                message = stringResource(id = R.string.login_signup_guest_recover_alert_message),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.login_signup_guest_recover_alert_confirm),
                        AlertDialogButtonData.positiveColor
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

    data class VerifyEmail(
        val email: String
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.info_email_confirm_alert_title),
                message = stringResource(id = R.string.info_email_confirm_alert_message, email),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.info_email_confirm_alert_button_resend),
                        AlertDialogButtonData.positiveColor
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

    object DeletePlace : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.place_list_menu_delete),
                message = stringResource(id = R.string.place_list_delete_place_alert_title),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.group_details_delete_group_alert_button),
                        AlertDialogButtonData.destructiveColor
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

    data class PlanLimit(
        val content: String,
        val limit: Int
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = R.string.plan_limit_alert_title),
                message = stringResource(id = R.string.plan_limit_alert_message, content, limit),
                buttons = listOf(
                    AlertDialogButtonData(
                        Buttons.buttonOk,
                        stringResource(id = R.string.button_ok),
                        AlertDialogButtonData.positiveColor
                    ),
                ),
                onAction = onAction
            )
        }
    }

    data class AppVersionInfo(
        val forceUpdate: Boolean
    ) : MainAlertDialog {
        override val key: String = this.javaClass.simpleName

        @Composable
        fun Show(onAction: (buttonId: String?) -> Unit) {
            MainAlertDialog(
                title = stringResource(id = if (forceUpdate) R.string.app_version_alert_title_mandatory else R.string.app_version_alert_title),
                message = stringResource(id = if (forceUpdate) R.string.app_version_alert_message_mandatory else R.string.app_version_alert_message),
                buttons = buildList {
                    add(
                        AlertDialogButtonData(
                            Buttons.buttonOk,
                            stringResource(id = R.string.app_version_alert_button_update),
                            AlertDialogButtonData.positiveColor
                        )
                    )
                    if (!forceUpdate) {
                        add(
                            AlertDialogButtonData(
                                Buttons.buttonCancel,
                                stringResource(id = R.string.button_cancel),
                                AlertDialogButtonData.destructiveColor
                            )
                        )
                    }
                },
                canDismiss = !forceUpdate,
                onAction = onAction
            )
        }
    }

}