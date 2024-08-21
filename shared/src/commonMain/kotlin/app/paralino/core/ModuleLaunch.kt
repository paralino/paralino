package app.paralino.core

import app.paralino.core.data.db.AppPrefs
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILogr

val appPrefs: AppPrefs get() = AppPrefs.instance
lateinit var logg: ILogr
lateinit var connectionStatus: IConnectionStatusProvider
