package app.paralino.core

import app.paralino.core.db.AppPrefs
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILogr
import app.paralino.core.provider.Logr

val appPrefs: AppPrefs get() = AppPrefs.instance
val logg: ILogr get() = Logr
lateinit var connectionStatus: IConnectionStatusProvider
