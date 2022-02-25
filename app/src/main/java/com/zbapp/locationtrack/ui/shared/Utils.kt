package com.zbapp.locationtrack.ui.shared

import android.content.Context
import android.text.SpannableString
import com.zbapp.core.utils.exceptions.*
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.utils.extensions.attributedBold

fun Context.errorString(error: Throwable?): SpannableString {
    return when (error) {
        is UnverifiedUserException -> getString(R.string.info_email_confirm)
        is RemoteConnectionException -> getString(R.string.error_server_unavailable)
        is InternalConnectionException -> getString(R.string.error_no_connection_snackbar)
        is BaseRemoteException -> {
            if (error.code != null)
                getString(R.string.error_something_wrong_http, error.code)
            else getString(R.string.error_something_wrong)
        }
        is MissingCryptoKeyException -> {
            when (error) {
                is MissingGroupKeyException -> getString(R.string.error_crypto_key_missing, "group")
                is MissingMemberKeyException -> getString(R.string.error_crypto_key_missing, "member")
                else -> getString(R.string.error_crypto_key_missing, "-")
            }
        }
        is EncryptionException -> getString(R.string.error_crypto_encryption_generic)
        is DecryptionException -> getString(R.string.error_crypto_decryption_generic)
        else -> getString(R.string.error_generic_snacbar)
    }.attributedBold(this)
}