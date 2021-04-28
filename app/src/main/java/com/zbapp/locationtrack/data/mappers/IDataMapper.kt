package com.zbapp.locationtrack.data.mappers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface IDataMapper<ONE, TWO> {
    fun map(model: ONE): TWO
    fun mapReversed(model: TWO): ONE
}

inline fun <T, reified U> IDataMapper<T, U>.mapFlowOfList(flow: Flow<List<T>>): Flow<List<U>> {
    return flow.map { it.map { this.map(it) }.toTypedArray() }
        .distinctUntilChanged { old, new ->
            old.contentEquals(new)
        }.map { it.toList() }
}