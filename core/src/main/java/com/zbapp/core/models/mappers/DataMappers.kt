package com.zbapp.core.models.mappers

import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe

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

interface IDataMapperLocal<LOCAL, REMOTE> {
    suspend fun local2Remote(model: LOCAL): REMOTE
    suspend fun remote2Local(model: REMOTE): LOCAL
}