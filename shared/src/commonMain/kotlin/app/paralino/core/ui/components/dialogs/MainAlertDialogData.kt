package app.paralino.core.ui.components.dialogs

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import app.paralino.core.MR
import app.paralino.core.models.user.BlocklistType
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.StringResource
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@Stable
@Serializable
data class AlertDialogButtonData(
    val id: String,
    val text: String,
    @Contextual val color: Color = Color.Transparent,
    val needsLongConfirmation: Boolean = false,
) {
    companion object {
        val destructiveColor = AppColors.errorBackground
        val positiveColor = AppColors.accentLight.copy(alpha = 0.3f)
    }
}

@Stable
@Serializable
data class AlertDialogData(
    val id: String,
    val title: String,
    val message: String,
    val canDismiss: Boolean = true,
    val buttons: List<AlertDialogButtonData>,
)

@Stable
@Serializable
data class AlertDialogAction(
    val dialog: MainAlertDialog,
    val action: String?,
)

@Serializable
sealed interface MainAlertDialog {
    object Buttons {
        const val buttonOk = "ok"
        const val buttonCancel = "cancel"
    }

    val key: String
    val dialogData: AlertDialogData

    fun stringResource(resource: StringResource): String {
        return resource.localizedString()
    }

    object ComingSoon : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"
        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_comingSoon_title),
            message = stringResource(resource = MR.strings.alert_comingSoon_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_cool),
                    color = AlertDialogButtonData.positiveColor
                )
            ),
        )
    }

    object Logout : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"
        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.settings_account_logout_alert_title),
            message = stringResource(resource = MR.strings.settings_account_logout_alert_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_ok),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    Buttons.buttonCancel,
                    stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object DeleteAccount : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.settings_account_delete_alert_title),
            message = stringResource(resource = MR.strings.settings_account_delete_alert_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_ok),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    Buttons.buttonCancel,
                    stringResource(resource = MR.strings.button_cancel)
                ),
            )
        )
    }

    object DeleteAccountConfirm : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = stringResource(resource = MR.strings.settings_account_delete_alert_confirm_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonCancel.plus("1"),
                    text = stringResource(resource = MR.strings.settings_account_delete_alert_cancel_button1)
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel.plus("2"),
                    text = stringResource(resource = MR.strings.settings_account_delete_alert_cancel_button2)
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.settings_account_delete_alert_confirm_button),
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel.plus("3"),
                    text = stringResource(resource = MR.strings.settings_account_delete_alert_cancel_button3)
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel.plus("4"),
                    text = stringResource(resource = MR.strings.settings_account_delete_alert_cancel_button4)
                ),
            ).shuffled(),
        )
    }

    object LeaveGroup : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.group_details_menu_leave),
            message = stringResource(resource = MR.strings.group_details_leave_group_alert_title),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.group_details_leave_group_alert_button),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object DeleteGroup : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.group_details_menu_delete),
            message = stringResource(resource = MR.strings.group_details_delete_group_alert_title),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.group_details_delete_group_alert_button),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object BlockGroup : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.group_details_menu_block),
            message = stringResource(resource = MR.strings.group_details_block_group_alert_title),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.group_details_block_group_alert_button),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    data class BlockUser(
        val userId: String
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.group_members_block_user),
            message = stringResource(resource = MR.strings.group_members_block_user_alert_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.group_details_block_group_alert_button),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    data class UnblockGroupOrUser(
        val id: String,
        val type: BlocklistType
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = when (type) {
                BlocklistType.GROUP -> stringResource(resource = MR.strings.blocklist_unblock_group_alert_title)
                BlocklistType.USER -> stringResource(resource = MR.strings.blocklist_unblock_user_alert_title)
                BlocklistType.UNKNOWN -> stringResource(resource = MR.strings.blocklist_unblock_alert_button)
            },
            message = when (type) {
                BlocklistType.GROUP -> stringResource(resource = MR.strings.blocklist_unblock_group_alert_message)
                BlocklistType.USER -> stringResource(resource = MR.strings.blocklist_unblock_user_alert_message)
                BlocklistType.UNKNOWN -> stringResource(resource = MR.strings.blocklist_unblock_alert_unknown)
            },
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.blocklist_unblock_alert_button),
                    color = AlertDialogButtonData.positiveColor
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    data class DeleteGroupMember(
        val memberUserId: String,
        val memberName: String,
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = MR.strings.group_members_remove_alert_title.localizedString(memberName),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.group_details_remove_member_alert_button),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    data class DeleteMemberLocation(
        val memberId: String,
        val locationId: String,
        val isUserOwnDevice: Boolean,
        val memberName: String,
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = if (isUserOwnDevice) {
                stringResource(resource = MR.strings.group_members_device_own_remove_alert_title)
            } else {
                MR.strings.group_members_device_other_remove_alert_title.localizedString(memberName)
            },
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.group_details_remove_member_alert_button),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    data class ToggleMemberVerification(val turnOn: Boolean) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = if (turnOn) {
                stringResource(resource = MR.strings.group_settings_verification_dialog_message_on)
            } else {
                stringResource(resource = MR.strings.group_settings_verification_dialog_message_off)
            },
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(MR.strings.button_confirm),
                    color = if (turnOn) {
                        AlertDialogButtonData.positiveColor
                    } else {
                        AlertDialogButtonData.destructiveColor
                    },
                    needsLongConfirmation = !turnOn
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object SharingBackgroundLocationMissing : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.groups_sharing_potential_error_title),
            message = stringResource(resource = MR.strings.groups_sharing_potential_error_background_location_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_ok),
                    color = AlertDialogButtonData.positiveColor
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    data class SharingErrors(val errorMessages: List<String>) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.groups_sharing_potential_error_title),
            message = MR.strings.groups_sharing_potential_error_message.localizedString(
                errorMessages.joinToString(separator = "**\n- **", prefix = "- **", postfix = "**")
            ),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_ok),
                    color = AlertDialogButtonData.positiveColor
                )
            ),
        )
    }

    data class PersonalCodeDelete(
        val codeId: String
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = stringResource(resource = MR.strings.invite_delete_alert_title),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_ok),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            )
        )
    }

    object SessionExpired : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_sessionExpired_title),
            message = stringResource(resource = MR.strings.alert_sessionExpired_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_ok),
                    color = AlertDialogButtonData.positiveColor
                )
            ),
        )
    }

    object RecoveryResetAccount : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = stringResource(resource = MR.strings.recover_account_alert_message_reset),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_confirm),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object RecoveryRecoverAccount : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = stringResource(resource = MR.strings.recover_account_alert_message_recover),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_confirm),
                    color = AlertDialogButtonData.positiveColor
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object GuestAccountCreating : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.login_signup_guest_alert_title),
            message = stringResource(resource = MR.strings.login_signup_guest_alert_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_confirm),
                    color = AlertDialogButtonData.positiveColor
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object GuestAccountRecover : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.login_signup_guest_recover_alert_title),
            message = stringResource(resource = MR.strings.login_signup_guest_recover_alert_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.login_signup_guest_recover_alert_confirm),
                    color = AlertDialogButtonData.positiveColor
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
            canDismiss = false
        )
    }

    object GuestAccountDismissConfirmation : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.alert_title_confirm),
            message = stringResource(resource = MR.strings.login_signup_guest_dismiss_alert_message),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.login_signup_guest_dismiss_alert_confirm),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
            canDismiss = false
        )
    }

    data class VerifyEmail(
        val email: String
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.info_email_confirm_alert_title),
            message = MR.strings.info_email_confirm_alert_message.localizedString(email),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.info_email_confirm_alert_button_resend),
                    color = AlertDialogButtonData.positiveColor
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    object DeletePlace : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.place_list_menu_delete),
            message = stringResource(resource = MR.strings.place_list_delete_place_alert_title),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.group_details_delete_group_alert_button),
                    color = AlertDialogButtonData.destructiveColor,
                    needsLongConfirmation = true
                ),
                AlertDialogButtonData(
                    id = Buttons.buttonCancel,
                    text = stringResource(resource = MR.strings.button_cancel)
                ),
            ),
        )
    }

    data class PlanLimit(
        val content: String,
        val limit: Int
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.plan_limit_alert_title),
            message = MR.strings.plan_limit_alert_message.localizedString(content, limit),
            buttons = listOf(
                AlertDialogButtonData(
                    id = Buttons.buttonOk,
                    text = stringResource(resource = MR.strings.button_ok),
                    color = AlertDialogButtonData.positiveColor
                ),
            ),
        )
    }

    data class AppVersionInfo(
        val forceUpdate: Boolean
    ) : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = if (forceUpdate) MR.strings.app_version_alert_title_mandatory else MR.strings.app_version_alert_title),
            message = stringResource(resource = if (forceUpdate) MR.strings.app_version_alert_message_mandatory else MR.strings.app_version_alert_message),
            buttons = buildList {
                add(
                    AlertDialogButtonData(
                        id = Buttons.buttonOk,
                        text = stringResource(resource = MR.strings.app_version_alert_button_update),
                        color = AlertDialogButtonData.positiveColor
                    )
                )
                if (!forceUpdate) {
                    add(
                        AlertDialogButtonData(
                            id = Buttons.buttonCancel,
                            text = stringResource(resource = MR.strings.button_cancel),
                            color = AlertDialogButtonData.destructiveColor
                        )
                    )
                }
            },
            canDismiss = !forceUpdate,
        )
    }

    object UnverifiedGroupInfo : MainAlertDialog {
        override val key: String = this::class.simpleName ?: "dialog"

        override val dialogData: AlertDialogData = AlertDialogData(
            id = key,
            title = stringResource(resource = MR.strings.groups_sharing_error_unverified_group_title),
            message = stringResource(resource = MR.strings.groups_sharing_error_unverified_group_message),
            buttons = buildList {
                add(
                    AlertDialogButtonData(
                        id = Buttons.buttonOk,
                        text = stringResource(resource = MR.strings.button_ok),
                        color = AlertDialogButtonData.positiveColor
                    )
                )
            },
        )
    }
}