package app.paralino.core

import app.paralino.core.data.db.AppConfig
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILogr

lateinit var config: AppConfig
lateinit var logg: ILogr
lateinit var connectionStatus: IConnectionStatusProvider
