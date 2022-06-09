package com.zbapp.remote.appwrite

import com.zbapp.core.models.raw.FunctionExecutionRaw
import com.zbapp.core.repo.api.IFunctionsApi

class FunctionsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IFunctionsApi {

    override suspend fun createExecution(functionId: String, data: String?): String {
        return remoteModule.execute {
            functions.createExecution(
                functionId = functionId,
                data = data,
                async = true
            ).id
        }
    }

    override suspend fun getExecution(
        functionId: String,
        executionId: String
    ): FunctionExecutionRaw {
        return remoteModule.execute {
            functions.getExecution(functionId, executionId).let {
                FunctionExecutionRaw(
                    id = it.id,
                    functionId = it.functionId,
                    status = it.status,
                    statusCode = it.statusCode,
                    response = it.response,
                    stderr = it.stderr
                )
            }
        }
    }

    override suspend fun runAndWait(functionId: String, data: String?): FunctionExecutionRaw {
        return remoteModule.execute {
            functions.createExecution(
                functionId = functionId,
                data = data,
                async = false
            ).let {
                FunctionExecutionRaw(
                    id = it.id,
                    functionId = it.functionId,
                    status = it.status,
                    statusCode = it.statusCode,
                    response = it.response,
                    stderr = it.stderr
                )
            }
        }
    }

}