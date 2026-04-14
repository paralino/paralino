package app.paralino.core.utils.extensions

import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.raw.FunctionErrorBody
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.UnauthorizedException

fun BaseRemoteException.Companion.fromRemote(
    code: Int?,
    error: FunctionErrorBody?,
): BaseRemoteException {
    return fromCode(
        code = code,
        message = error?.message,
        cause = null,
        type = error?.type
    )
}

val UnauthorizedException.isUserBlocked: Boolean get() = type == UserSessionMonitor.userBlocked