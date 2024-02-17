package app.paralino.core

import app.paralino.core.data.db.AppConfig
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILogr

val config: AppConfig get() = AppConfig.instance
lateinit var logg: ILogr
lateinit var connectionStatus: IConnectionStatusProvider
