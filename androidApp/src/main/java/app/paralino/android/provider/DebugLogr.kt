/*
 * Copyright (c) 2020 - Zelimir
 */

package app.paralino.android.provider

import android.util.Log
import app.paralino.core.provider.LogPriority
import app.paralino.core.provider.SystemLogr

internal open class DebugLogr(
    val enabled: Boolean,
    private val tagPrefix: String = ""
) : SystemLogr() {

    private val MAX_LOG_LENGTH = 4000
    override fun systemLog(priority: LogPriority, tag: String?, message: String, t: Throwable?) {

        if (!enabled) {
            return
        }

        val androidPriority = when(priority) {
            LogPriority.VERBOSE -> Log.VERBOSE
            LogPriority.DEBUG -> Log.DEBUG
            LogPriority.INFO -> Log.INFO
            LogPriority.WARN -> Log.WARN
            LogPriority.ERROR -> Log.ERROR
        }
        val debugTag = getDebugTag(tag)

        if (message.length < MAX_LOG_LENGTH) {
            Log.println(androidPriority, debugTag, message)
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
                Log.println(androidPriority, debugTag, part)
                i = end
            } while (i < newline)
            i++
        }
    }


    private fun getDebugTag(userTag: String?): String {
        val tag = userTag ?: tagPrefix
        val stackTrace = Throwable().stackTrace
        val stackTraceTag = stackTrace.take(7).reversed().take(2).mapIndexed { index, it ->
            if(index == 0)
                "${it.fileName}:${it.methodName}:${it.lineNumber}"
            else "${it.methodName}:${it.lineNumber}"
        }.joinToString(separator = "-")
        return "$tag($stackTraceTag)".take(80)
    }

}
