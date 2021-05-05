package com.zbapp.locationtrack.utils.logger

import android.util.Log
import com.zbapp.core.utils.ILogger

abstract class LoggerKind: ILogger {
    internal val explicitTag = ThreadLocal<String>()

    override fun tag(tag: String): ILogger {
        explicitTag.set(tag)
        return this
    }

    override fun getTag(): String? {
        val tag: String? = explicitTag.get()
        explicitTag.remove()
        return tag
    }

    override fun v(message: String, t: Throwable?) = prepareLog(Log.VERBOSE, t, message)
    override fun d(message: String, t: Throwable?) = prepareLog(Log.DEBUG, t, message)
    override fun i(message: String, t: Throwable?) = prepareLog(Log.INFO, t, message)
    override fun w(message: String, t: Throwable?) = prepareLog(Log.WARN, t, message)
    override fun e(message: String, t: Throwable?) = prepareLog(Log.ERROR, t, message)
    open fun wtf(message: String, t: Throwable? = null) = prepareLog(Log.ASSERT, t, message)
    open fun log(priority: Int, message: String, t: Throwable?) = prepareLog(priority, t, message)

    abstract fun log(priority: Int, tag: String?, message: String, t: Throwable?)

    open fun isLoggable(priority: Int): Boolean {
        return true
    }

    private fun prepareLog(priority: Int, t: Throwable?, message: String) {
        var outputMessage: String = message
        if (!isLoggable(priority)) {
            return
        }

        if (outputMessage.isEmpty()) {
            if (t == null) {
                return
            }
            outputMessage = getStackTraceString(t)
        } else if(t != null){
            outputMessage += "\n" + getStackTraceString(t)
        }

        log(priority, getTag(), outputMessage, t)
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = java.io.StringWriter(256)
        val pw = java.io.PrintWriter(sw, false)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
