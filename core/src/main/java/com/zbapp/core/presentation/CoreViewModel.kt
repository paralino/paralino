package com.zbapp.core.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

abstract class CoreViewModel(
    /**
     * TODO REPLACE!
     * This is temporarily here as lateinit var until we switch to KMM project
     */
    val coroutineScope: CoroutineScope
) {

    /**
     * Same as Android ViewModel onCleared
     */
    protected open fun onCleared() {
        coroutineScope.cancel()
    }
}