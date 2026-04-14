package app.paralino.core.utils.exceptions

import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.raw.FunctionErrorBody

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