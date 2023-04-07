package app.paralino.core.provider

abstract class IRemoteModule {

    fun <T> wrap(error: Throwable): T {
        return Result.failure<T>(error).getOrThrow()
    }

}