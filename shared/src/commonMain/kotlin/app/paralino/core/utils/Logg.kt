package app.paralino.core.utils

interface ILogger {
    fun tag(tag: String): ILogger
    fun getTag(): String?
    fun v(message: String, t: Throwable? = null)
    fun d(message: String, t: Throwable? = null)
    fun i(message: String, t: Throwable? = null)
    fun w(message: String, t: Throwable? = null)
    fun e(message: String, t: Throwable? = null)
    fun e(t: Throwable? = null) = e(t?.message ?: "Error", t)
}