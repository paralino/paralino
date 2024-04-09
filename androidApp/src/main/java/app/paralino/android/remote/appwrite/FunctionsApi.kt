package app.paralino.android.remote.appwrite

import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.api.BaseFunctionsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import io.appwrite.enums.ExecutionMethod

class FunctionsApi constructor(
    private val remoteModule: AppWriteRemoteModule,
    deviceApiProvider: IDeviceApiProvider
) : BaseFunctionsApi(deviceApiProvider), IFunctionsApi {

    override suspend fun runAsync(function: ServerFunction, data: String?): String {
        return remoteModule.execute {
            functions.createExecution(
                functionId = function.functionId,
                body = data,
                async = true,
                path = function.path,
                method = ExecutionMethod.entries.first { it.value == function.method },
                headers = mutableMapOf(clientHeader)
            ).id
        }
    }

    override suspend fun runAndWait(function: ServerFunction, data: String?): FunctionExecutionRaw {
        return remoteModule.execute {
            val execution = functions.createExecution(
                functionId = function.functionId,
                body = data,
                async = false,
                path = function.path,
                method = ExecutionMethod.entries.first { it.value == function.method },
                headers = mutableMapOf(clientHeader)
            )

            FunctionExecutionRaw(
                id = execution.id,
                functionId = execution.functionId,
                status = execution.status,
                statusCode = execution.responseStatusCode.toInt(),
                response = execution.responseBody,
                errors = execution.errors
            )
        }
    }
}