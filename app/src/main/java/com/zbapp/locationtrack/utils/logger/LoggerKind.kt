package com.zbapp.locationtrack.utils.logger

import android.util.Log

abstract class LoggerKind {
    internal val explicitTag = ThreadLocal<String>()

    open fun getTag(): String? {
        val tag: String? = explicitTag.get()
        explicitTag.remove()
        return tag
    }

    open fun v(message: String, t: Throwable? = null) = prepareLog(Log.VERBOSE, t, message)
    open fun d(message: String, t: Throwable? = null) = prepareLog(Log.DEBUG, t, message)
    open fun i(message: String, t: Throwable? = null) = prepareLog(Log.INFO, t, message)
    open fun w(message: String, t: Throwable? = null) = prepareLog(Log.WARN, t, message)
    open fun e(message: String, t: Throwable? = null) = prepareLog(Log.ERROR, t, message)
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
