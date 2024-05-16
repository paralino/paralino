package app.paralino.core.provider

import app.paralino.core.utils.exceptions.InternalConnectionException

actual fun Throwable.canLogExceptionStackTrace(): Boolean = this !is InternalConnectionException
actual fun shouldLogExceptionToMessage(): Boolean = true