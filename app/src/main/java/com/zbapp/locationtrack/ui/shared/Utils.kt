package com.zbapp.locationtrack.ui.shared

import androidx.annotation.StringRes
import com.zbapp.core.utils.exceptions.RemoteConnectionException
import com.zbapp.core.utils.exceptions.GenericServerException
import com.zbapp.core.utils.exceptions.InternalConnectionException
import com.zbapp.core.utils.exceptions.UnverifiedUserException
import com.zbapp.locationtrack.R

@StringRes
fun Throwable.errorStringRes(): Int? {
    return when(this) {
        is UnverifiedUserException -> R.string.info_email_confirm
        is RemoteConnectionException -> R.string.error_server_unavailable
        is InternalConnectionException -> R.string.error_no_connection_snackbar
        is GenericServerException -> R.string.error_something_wrong
        else -> null
    }
}