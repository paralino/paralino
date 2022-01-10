package com.zbapp.remote.appwrite

import com.zbapp.core.repo.api.IFunctionsApi

class FunctionsApi constructor(
    private val remoteModule: AppWriteRemoteModule
): IFunctionsApi {

    override suspend fun createExecution(functionId: String, data: String?): String {
        return remoteModule.execute {
            functions.createExecution(functionId, data).id
        }
    }

}