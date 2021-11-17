package com.zbapp.core

import com.zbapp.core.utils.IConfig
import com.zbapp.core.utils.ILogger
import kotlinx.coroutines.CoroutineScope

lateinit var config: IConfig
lateinit var logg: ILogger

interface ICoroutineScopeProvider {
    val defaultScope: CoroutineScope
    val defaultIOScope: CoroutineScope
}