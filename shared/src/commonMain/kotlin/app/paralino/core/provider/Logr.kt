package app.paralino.core.provider

abstract class SystemLogr {
    abstract fun systemLog(priority: LogPriority, tag: String?, message: String, t: Throwable?)
}

object Logr : LogrImpl()

abstract class LogrImpl : ILogr {
    var systemLogr: SystemLogr? = null
        private set

    fun setLogr(logr: SystemLogr) {
        this.systemLogr = logr
    }

    override fun log(priority: LogPriority, tag: String?, message: String, t: Throwable?) {
        val output = if (t != null) {
            message.takeIf { it.isNotEmpty() }
                ?.let {
                    "$it\n${t.stackTraceToString()}"
                }
                ?: t.stackTraceToString()
        } else {
            message.takeIf { it.isNotEmpty() } ?: return
        }

        systemLogr?.systemLog(priority, tag, output, t)
    }
}

interface ILogr {

    fun tag(tag: String): ILogrWithTag = ILogrWithTag(tag, this)

    open fun v(message: String) =
        log(priority = LogPriority.VERBOSE, tag = null, message = message, t = null)

    open fun d(message: String) =
        log(priority = LogPriority.DEBUG, tag = null, message = message, t = null)

    open fun i(message: String) =
        log(priority = LogPriority.INFO, tag = null, message = message, t = null)

    open fun w(message: String) =
        log(priority = LogPriority.WARN, tag = null, message = message, t = null)

    open fun e(message: String) =
        log(priority = LogPriority.ERROR, tag = null, message = message, t = null)

    fun v(tag: String, message: String) =
        log(priority = LogPriority.VERBOSE, tag = tag, message = message, t = null)

    fun d(tag: String, message: String) =
        log(priority = LogPriority.DEBUG, tag = tag, message = message, t = null)

    fun i(tag: String, message: String) =
        log(priority = LogPriority.INFO, tag = tag, message = message, t = null)

    fun w(tag: String, message: String) =
        log(priority = LogPriority.WARN, tag = tag, message = message, t = null)

    fun e(tag: String, message: String) =
        log(priority = LogPriority.ERROR, tag = tag, message = message, t = null)

    open fun v(message: String, t: Throwable?) =
        log(priority = LogPriority.VERBOSE, tag = null, message = message, t = t)

    open fun d(message: String, t: Throwable?) =
        log(priority = LogPriority.DEBUG, tag = null, message = message, t = t)

    open fun i(message: String, t: Throwable?) =
        log(priority = LogPriority.INFO, tag = null, message = message, t = t)

    open fun w(message: String, t: Throwable?) =
        log(priority = LogPriority.WARN, tag = null, message = message, t = t)

    open fun e(message: String, t: Throwable?) =
        log(priority = LogPriority.ERROR, tag = null, message = message, t = t)

    fun v(tag: String, message: String, t: Throwable?) =
        log(priority = LogPriority.VERBOSE, tag = tag, message = message, t = t)

    fun d(tag: String, message: String, t: Throwable?) =
        log(priority = LogPriority.DEBUG, tag = tag, message = message, t = t)

    fun i(tag: String, message: String, t: Throwable?) =
        log(priority = LogPriority.INFO, tag = tag, message = message, t = t)

    fun w(tag: String, message: String, t: Throwable?) =
        log(priority = LogPriority.WARN, tag = tag, message = message, t = t)

    fun e(tag: String, message: String, t: Throwable?) =
        log(priority = LogPriority.ERROR, tag = tag, message = message, t = t)

    fun e(t: Throwable?) = e(message = t?.message ?: "Error", t = t)

    abstract fun log(priority: LogPriority, tag: String?, message: String, t: Throwable?)
}

class ILogrWithTag(val tag: String, val logr: ILogr) : ILogr {

    override fun v(message: String) =
        log(priority = LogPriority.VERBOSE, tag = tag, message = message, t = null)

    override fun d(message: String) =
        log(priority = LogPriority.DEBUG, tag = tag, message = message, t = null)

    override fun i(message: String) =
        log(priority = LogPriority.INFO, tag = tag, message = message, t = null)

    override fun w(message: String) =
        log(priority = LogPriority.WARN, tag = tag, message = message, t = null)

    override fun e(message: String) =
        log(priority = LogPriority.ERROR, tag = tag, message = message, t = null)

    override fun v(message: String, t: Throwable?) =
        log(priority = LogPriority.VERBOSE, tag = tag, message = message, t = t)

    override fun d(message: String, t: Throwable?) =
        log(priority = LogPriority.DEBUG, tag = tag, message = message, t = t)

    override fun i(message: String, t: Throwable?) =
        log(priority = LogPriority.INFO, tag = tag, message = message, t = t)

    override fun w(message: String, t: Throwable?) =
        log(priority = LogPriority.WARN, tag = tag, message = message, t = t)

    override fun e(message: String, t: Throwable?) =
        log(priority = LogPriority.ERROR, tag = tag, message = message, t = t)

    override fun log(priority: LogPriority, tag: String?, message: String, t: Throwable?) {
        logr.log(priority, tag, message, t)
    }
}

enum class LogPriority {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}