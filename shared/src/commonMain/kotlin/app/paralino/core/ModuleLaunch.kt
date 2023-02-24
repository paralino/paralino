package app.paralino.core

import app.paralino.core.utils.IConfig
import app.paralino.core.utils.IConnectionStatusProvider
import app.paralino.core.utils.ILogger
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

lateinit var config: IConfig
lateinit var logg: ILogger
lateinit var connectionStatus: IConnectionStatusProvider

interface ICoroutineScopeProvider {
    val defaultScope: CoroutineScope
    val defaultIOScope: CoroutineScope
    val userSessionIOScope: CoroutineScope
    val defaultDispatcher: CoroutineContext
    val ioDispatcher: CoroutineContext
}