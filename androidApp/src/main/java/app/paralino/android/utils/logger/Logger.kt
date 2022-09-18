/*
 * Copyright (c) 2020 - Zelimir
 */

import app.paralino.android.utils.logger.LoggerImpl

object Logger: LoggerImpl()

fun logv(msg: String) = Logger.v(msg)
fun logv(tag: String, msg: String) = Logger.tag(tag).v(msg)
fun logd(msg: String, t: Throwable? = null) = Logger.d(msg, t)
fun logi(msg: String, t: Throwable? = null) = Logger.i(msg, t)
fun logw(msg: String, t: Throwable? = null) = Logger.w(msg, t)
fun loge(t: Throwable? = null) = Logger.e(t?.message ?: "Error", t)
fun loge(msg: String, t: Throwable? = null) = Logger.e(msg, t)
