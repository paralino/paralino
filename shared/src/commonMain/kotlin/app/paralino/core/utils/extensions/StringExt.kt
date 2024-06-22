package app.paralino.core.utils.extensions

import app.paralino.core.MR
import app.paralino.core.presentation.common.FormValidation
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
import app.paralino.core.utils.exceptions.RemoteConnectionException
import app.paralino.core.utils.exceptions.UnverifiedUserException

fun String.splitRanges(wrappedChars: String): List<Pair<String, IntRange>> {
    val ranges = mutableListOf<Pair<String, IntRange>>()
    var isWrapped = false
    var start = 0
    this.split(wrappedChars).forEach {
        if(isWrapped) ranges.add(it to IntRange(start, start + it.length))
        isWrapped = !isWrapped
        start += it.length
    }
    return ranges.filter { pair ->
        val it = pair.second
        it.first >= 0 && it.last <= this.length && it.first < it.last
    }
}

fun Throwable?.errorString(): String {
    val error = this
    return when (error) {
        is UnverifiedUserException -> MR.strings.info_email_confirm.localizedString()
        is RemoteConnectionException -> MR.strings.error_server_unavailable.localizedString()
        is InternalConnectionException -> MR.strings.error_no_connection_snackbar.localizedString()
        is BaseRemoteException -> {
            when {
                error.code != null -> {
                    MR.strings.error_something_wrong_http.localizedString(error.code)
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
        else -> MR.strings.error_generic_snacbar.localizedString()
    }
}

fun getFormValidationErrorText(validation: FormValidation): String {
    return when (validation) {
        FormValidation.Email -> MR.strings.form_invalid_email_description.localizedString()
        FormValidation.Password -> MR.strings.form_invalid_password_length_description.localizedString()
        FormValidation.PasswordConfirm -> MR.strings.form_invalid_password_match_description.localizedString()
        FormValidation.TextNotEmpty -> MR.strings.form_invalid_name_description.localizedString()
    }
}