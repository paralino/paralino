package com.zbapp.remote.appwrite

import com.zbapp.core.models.raw.FunctionExecutionRaw
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.poll

class FunctionsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IFunctionsApi {

    override suspend fun createExecution(functionId: String, data: String?): String {
        return remoteModule.execute {
            functions.createExecution(functionId, data).id
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
                    stdout = it.stdout,
                    stderr = it.stderr
                )
            }
        }
    }

    override suspend fun runAndWait(functionId: String, data: String?): FunctionExecutionRaw {
        val id = createExecution(functionId, data)
        return poll {
            getExecution(functionId, id).takeIf { it.isCompleted }
        }
    }

}