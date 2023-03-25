package app.paralino.core

import app.paralino.core.provider.IConfig
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILogr

lateinit var config: IConfig
lateinit var logg: ILogr
lateinit var connectionStatus: IConnectionStatusProvider
