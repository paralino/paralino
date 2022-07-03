package app.paralino.mobile.ui.shared

import android.content.Context
import android.content.res.Resources
import android.text.SpannableString
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.utils.exceptions.*
import app.paralino.mobile.R
import app.paralino.mobile.utils.extensions.attributedBold

fun Context.errorString(error: Throwable?): SpannableString {
    return errorStringClean(error).attributedBold(this)
}

fun Context.errorStringClean(error: Throwable?): String {
    return error.errorString(this)
}

fun Throwable?.errorString(context: Context): String {
    return this.errorString(context.resources)
}

fun Throwable?.errorString(resources: Resources): String {
    val error = this
    return when (error) {
        is UnverifiedUserException -> resources.getString(R.string.info_email_confirm)
        is RemoteConnectionException -> resources.getString(R.string.error_server_unavailable)
        is InternalConnectionException -> resources.getString(R.string.error_no_connection_snackbar)
        is BaseRemoteException -> {
            if (error.code != null)
                resources.getString(R.string.error_something_wrong_http, error.code)
            else resources.getString(R.string.error_something_wrong)
        }
        is MissingCryptoKeyException -> {
            when (error) {
                is MissingGroupKeyException -> resources.getString(R.string.error_crypto_key_missing, "group")
                is MissingMemberKeyException -> resources.getString(R.string.error_crypto_key_missing, "member")
                is MissingUserKeyException -> resources.getString(R.string.error_crypto_key_missing_user)
                else -> resources.getString(R.string.error_crypto_key_missing, "-")
            }
        }
        is EncryptionException -> resources.getString(R.string.error_crypto_encryption_generic)
        is DecryptionException -> resources.getString(R.string.error_crypto_decryption_generic)
        else -> resources.getString(R.string.error_generic_snacbar)
    }
}

@Composable
fun Throwable?.errorString(): String {
    return this.errorString(LocalContext.current)
}

fun getFormValidationErrorText(context: Context, validation: FormValidation): String {
    return when (validation) {
        FormValidation.Email -> context.getString(R.string.form_invalid_email_description)
        FormValidation.Password -> context.getString(R.string.form_invalid_password_length_description)
        FormValidation.PasswordConfirm -> context.getString(R.string.form_invalid_password_match_description)
        FormValidation.TextNotEmpty -> context.getString(R.string.form_invalid_name_description)
    }
}