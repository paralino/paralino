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
        val tag = super.getTag() ?: tagPrefix
        val stackTrace = Throwable().stackTrace
        val stackTraceTag = stackTrace.take(7).reversed().take(2).mapIndexed { index, it ->
            if(index == 0)
                "${it.fileName}:${it.methodName}:${it.lineNumber}"
            else "${it.methodName}:${it.lineNumber}"
        }.joinToString(separator = "-")
        return "$tag($stackTraceTag)".take(80)
    }

}
