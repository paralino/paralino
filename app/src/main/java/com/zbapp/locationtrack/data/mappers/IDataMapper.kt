package com.zbapp.locationtrack.data.mappers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface IDataMapper<CORE, LOCAL, REMOTE> {
    fun core2Local(model: CORE): LOCAL
    fun core2Remote(model: CORE): REMOTE
    fun local2Core(model: LOCAL): CORE
    fun local2Remote(model: LOCAL): REMOTE
    fun remote2Core(model: REMOTE): CORE
    fun remote2Local(model: REMOTE): LOCAL
}

inline fun <reified CORE, LOCAL, REMOTE> IDataMapper<CORE, LOCAL, REMOTE>.mapFlowOfList(flow: Flow<List<LOCAL>>): Flow<List<CORE>> {
    return flow.map { it.map { this.local2Core(it) }.toTypedArray() }
        .distinctUntilChanged { old, new ->
            old.contentEquals(new)
        }.map { it.toList() }
}