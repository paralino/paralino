package app.paralino.core.utils.extensions

import app.paralino.core.resources.moko.*
import app.paralino.core.models.settings.FormValidation
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.DataStillLoadingException
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.EncryptionException
import app.paralino.core.utils.exceptions.InternalConnectionException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.exceptions.MissingUserKeyException
import app.paralino.core.utils.exceptions.PermissionDeniedException
import app.paralino.core.utils.exceptions.RemoteConnectionException
import app.paralino.core.utils.exceptions.UnverifiedUserException

fun Throwable?.errorString(): String {
    val error = this
    return when (error) {
        is UnverifiedUserException -> MR.strings.info_email_confirm.localizedString()
        is RemoteConnectionException -> {
            when (error.code) {
                429 -> MR.strings.group_members_status_too_many_requests.localizedString()
                else -> MR.strings.error_server_unavailable.localizedString()
            }
        }
        is InternalConnectionException -> MR.strings.error_no_connection_snackbar.localizedString()
        is BaseRemoteException -> {
            when {
                error.code != null -> {
                    MR.strings.error_something_wrong_http.localizedString(error.code!!)
                }
                error is DataStillLoadingException -> {
                    MR.strings.error_still_loading_snackbar.localizedString()
                }
                else -> {
                    MR.strings.error_something_wrong.localizedString()
                }
            }
        }
        is MissingCryptoKeyException -> {
            when (error) {
                is MissingGroupKeyException -> MR.strings.error_crypto_key_missing.localizedString("group")
                is MissingMemberKeyException -> MR.strings.error_crypto_key_missing.localizedString("member")
                is MissingUserKeyException -> MR.strings.error_crypto_key_missing_user.localizedString()
                else -> MR.strings.error_crypto_key_missing.localizedString("-")
            }
        }
        is EncryptionException -> MR.strings.error_crypto_encryption_generic.localizedString()
        is DecryptionException -> MR.strings.error_crypto_decryption_generic.localizedString()
        is PermissionDeniedException -> {
            if (error.type == "plan_limit_reached") MR.strings.plan_limit_generic_message.localizedString()
            else MR.strings.error_permissionDenied_remote.localizedString()
        }
        else -> MR.strings.error_generic_snacbar.localizedString()
    }
}

fun getFormValidationErrorText(validation: FormValidation): String {
    return when (validation) {
        FormValidation.Email -> MR.strings.form_invalid_email_description.localizedString()
        FormValidation.Password -> MR.strings.form_invalid_password_length_description.localizedString()
        FormValidation.PasswordConfirm -> MR.strings.form_invalid_password_match_description.localizedString()
        FormValidation.TextNotEmpty -> MR.strings.form_generic_validation.localizedString()
    }
}