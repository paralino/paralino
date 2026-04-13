package app.paralino.core.provider

import app.paralino.core.utils.exceptions.InternalConnectionException
import app.paralino.core.utils.exceptions.TimeoutException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

actual fun Throwable.canLogExceptionStackTrace(): Boolean = this !is SocketTimeoutException && this !is InternalConnectionException && this !is ConnectException
actual fun Throwable.isConnectionError(): Boolean = this is UnknownHostException || this is InternalConnectionException || this is ConnectException
actual fun Throwable.isSocketTimeout(): Boolean = this is SocketTimeoutException || this is TimeoutException
actual fun shouldLogExceptionToMessage(): Boolean = false