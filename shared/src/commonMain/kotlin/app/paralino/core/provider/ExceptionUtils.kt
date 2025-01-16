package app.paralino.core.provider

expect fun Throwable.canLogExceptionStackTrace(): Boolean
expect fun Throwable.isConnectionError(): Boolean
expect fun Throwable.isSocketTimeout(): Boolean
expect fun shouldLogExceptionToMessage(): Boolean