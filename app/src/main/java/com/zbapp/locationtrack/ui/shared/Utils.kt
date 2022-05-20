package com.zbapp.locationtrack.ui.shared

import android.content.Context
import android.content.res.Resources
import android.text.SpannableString
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zbapp.core.utils.exceptions.*
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.utils.extensions.attributedBold

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