package app.paralino.core.provider

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

interface ICoroutineScopeProvider {
    val defaultScope: CoroutineScope
    val defaultIOScope: CoroutineScope
    val userSessionIOScope: CoroutineScope
    val defaultDispatcher: CoroutineContext
    val ioDispatcher: CoroutineContext
}