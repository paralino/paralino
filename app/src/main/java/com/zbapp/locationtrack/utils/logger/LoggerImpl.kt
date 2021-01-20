/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.utils.logger

open class LoggerImpl: LoggerKind() {

    private var LOGGER: LoggerKind? = null
    private var lock: Any = Any()

    fun setLogger(loggerKind: LoggerKind?) {
        if (loggerKind == null) {
            throw NullPointerException("loggerKind == null")
        }
        synchronized (lock) {
            LOGGER = loggerKind
        }
    }

    /** Set a one-time tag for use on the next logging call. */
    fun tag(tag: String): LoggerKind {
        LOGGER?.explicitTag?.set(tag)
        return this
    }

    override fun v(message: String, t: Throwable?) {
        LOGGER?.v(message, t)
    }

    override fun d(message: String, t: Throwable?) {
        LOGGER?.d(message, t)
    }

    override fun i(message: String, t: Throwable?) {
        LOGGER?.i(message, t)
    }

    override fun w(message: String, t: Throwable?) {
        LOGGER?.w(message, t)
    }

    override fun e(message: String, t: Throwable?) {
        LOGGER?.e(message, t)
    }

    override fun wtf(message: String, t: Throwable?) {
        LOGGER?.wtf(message, t)
    }

    override fun log(priority: Int, message: String, t: Throwable?) {
        LOGGER?.log(priority, message, t)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        tag?.let { tag(it) }
        log(priority, message, t)
    }
}
