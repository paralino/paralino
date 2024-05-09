package app.paralino.core.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class CoroutineScopeProvider: ICoroutineScopeProvider {
    override val defaultScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override val defaultIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val userSessionIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val defaultDispatcher: CoroutineContext get() = Dispatchers.Default
    override val ioDispatcher: CoroutineContext get() = Dispatchers.IO
}