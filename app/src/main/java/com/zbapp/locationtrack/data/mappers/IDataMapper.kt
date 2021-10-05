package com.zbapp.locationtrack.data.mappers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface IDataMapperLocal<LOCAL, REMOTE> {
    suspend fun local2Remote(model: LOCAL): REMOTE
    suspend fun remote2Local(model: REMOTE): LOCAL
}

inline fun <LOCAL, reified REMOTE> IDataMapperLocal<LOCAL, REMOTE>.mapFlowOfList(flow: Flow<List<LOCAL>>): Flow<List<REMOTE>> {
    return flow.map { it.map { this.local2Remote(it) }.toTypedArray() }
        .distinctUntilChanged { old, new ->
            old.contentEquals(new)
        }.map { it.toList() }
}