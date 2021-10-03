package com.zbapp.locationtrack.ui.shared

import androidx.annotation.StringRes
import com.zbapp.core.utils.*
import com.zbapp.locationtrack.R

@StringRes
fun Throwable.errorStringRes(): Int? {
    return when(this) {
        is UnverifiedUserException -> R.string.info_email_confirm
        is ConnectionException -> R.string.error_server_unavailable
        is GenericServerException -> R.string.error_something_wrong
        else -> null
    }
}