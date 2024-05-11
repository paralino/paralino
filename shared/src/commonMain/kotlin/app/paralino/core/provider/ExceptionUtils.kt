package app.paralino.core.provider

expect fun Throwable.canLogExceptionStackTrace(): Boolean
expect fun shouldLogExceptionToMessage(): Boolean