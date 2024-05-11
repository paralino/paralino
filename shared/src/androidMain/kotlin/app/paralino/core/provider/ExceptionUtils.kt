package app.paralino.core.provider

import app.paralino.core.utils.exceptions.InternalConnectionException
import java.net.ConnectException
import java.net.SocketTimeoutException

actual fun Throwable.canLogExceptionStackTrace(): Boolean = this !is SocketTimeoutException && this !is InternalConnectionException && this !is ConnectException
actual fun shouldLogExceptionToMessage(): Boolean = false