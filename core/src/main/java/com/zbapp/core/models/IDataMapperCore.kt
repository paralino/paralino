package com.zbapp.core.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface IDataMapperCore<CORE, REMOTE> {
    suspend fun remote2Core(model: REMOTE): CORE
    suspend fun core2Remote(model: CORE): REMOTE
}

fun <REMOTE, CORE> Flow<REMOTE>.decrypt(mapper: IDataMapperCore<CORE, REMOTE>): Flow<CORE> {
    return this.map { mapper.remote2Core(it) }
}

fun <REMOTE, CORE> Flow<CORE>.encrypt(mapper: IDataMapperCore<CORE, REMOTE>): Flow<REMOTE> {
    return this.map { mapper.core2Remote(it) }
}


fun <REMOTE, CORE> Flow<List<CORE>>.encryptList(mapper: IDataMapperCore<CORE, REMOTE>): Flow<List<REMOTE>> {
    return this.map { it.encryptList(mapper) }
}

suspend fun <REMOTE, CORE> List<CORE>.encryptList(mapper: IDataMapperCore<CORE, REMOTE>): List<REMOTE> {
    return this.map { mapper.core2Remote(it) }
}


fun <REMOTE, CORE> Flow<List<REMOTE>>.decryptList(mapper: IDataMapperCore<CORE, REMOTE>): Flow<List<CORE>> {
    return this.map { it.decryptList(mapper) }
}

suspend fun <REMOTE, CORE> List<REMOTE>.decryptList(mapper: IDataMapperCore<CORE, REMOTE>): List<CORE> {
    return this.map { mapper.remote2Core(it) }
}