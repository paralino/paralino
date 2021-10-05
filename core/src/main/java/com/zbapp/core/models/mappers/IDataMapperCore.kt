package com.zbapp.core.models.mappers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface IDataMapperCore<CORE, REMOTE> {
    /** copy id of a model to be used internally in the app.
     * We don't have access to object id before data is uploaded to backend.
     * And in order not to decrypt and encrypt data every time we get it, id is applied here when data is read and decrypted
     * If encrypted object was never updated, id will be empty in database (in data field)
     * That is why we are always mapping the id
     * */
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