package app.paralino.core.provider

import app.paralino.core.utils.exceptions.InternalConnectionException
import app.paralino.core.utils.exceptions.TimeoutException

actual fun Throwable.canLogExceptionStackTrace(): Boolean = this !is InternalConnectionException
actual fun Throwable.isConnectionError(): Boolean = this is InternalConnectionException
actual fun Throwable.isSocketTimeout(): Boolean = this is TimeoutException
actual fun shouldLogExceptionToMessage(): Boolean = true