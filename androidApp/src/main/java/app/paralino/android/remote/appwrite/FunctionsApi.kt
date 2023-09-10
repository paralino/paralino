package app.paralino.android.remote.appwrite

import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.repo.api.IFunctionsApi

class FunctionsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IFunctionsApi {

    override suspend fun createExecution(functionId: String, data: String?): String {
        return remoteModule.execute {
            functions.createExecution(
                functionId = functionId,
                body = data,
                async = true
            ).id
        }
    }

    override suspend fun runAndWait(functionId: String, data: String?): FunctionExecutionRaw {
        return remoteModule.execute {
            functions.createExecution(
                functionId = functionId,
                body = data,
                async = false
            ).let {
                FunctionExecutionRaw(
                    id = it.id,
                    functionId = it.functionId,
                    status = it.status,
                    statusCode = it.responseStatusCode.toInt(),
                    response = it.responseBody,
                    stderr = it.errors
                )
            }
        }
    }

}