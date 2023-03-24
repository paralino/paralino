package app.paralino.core

import app.paralino.core.provider.IConfig
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILogger

lateinit var config: IConfig
lateinit var logg: ILogger
lateinit var connectionStatus: IConnectionStatusProvider
