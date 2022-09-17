/*
 * Copyright (c) 2020 - Zelimir
 */

package app.paralino.android.utils.logger

import android.util.Log

internal open class DebugLogger(private val tagPrefix: String = "") : LoggerKind() {
    private val MAX_LOG_LENGTH = 4000
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (message.length < MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                Log.wtf(tag, message)
            } else {
                Log.println(priority, tag, message)
            }
            return
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = Math.min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, part)
                } else {
                    Log.println(priority, tag, part)
                }
                i = end
            } while (i < newline)
            i++
        }
    }


    override fun getTag(): String {
        val tag = super.getTag()
        if (tag != null) {
            return tag
        }

        val stackTrace = Throwable().stackTrace
        if (stackTrace.size <= 7) {
            throw IllegalStateException(
                    "Stacktrace didn't have enough elements: proguard?")
        }
        return "$tagPrefix(${stackTrace[6].fileName}:${stackTrace[6].lineNumber}) ${stackTrace[6].methodName}()"
    }

}
