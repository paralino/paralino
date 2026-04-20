package app.paralino.android.remote.appwrite

import app.paralino.core.logg
import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.api.BaseFunctionsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import io.appwrite.enums.ExecutionMethod
import kotlinx.coroutines.delay
import java.net.UnknownHostException

class FunctionsApi constructor(
    private val remoteModule: AppWriteRemoteModule,
    deviceApiProvider: IDeviceApiProvider
) : BaseFunctionsApi(deviceApiProvider), IFunctionsApi {

    override suspend fun runAsync(function: ServerFunction, data: String?): String {
        logg.d("server_function", "Call: ${function.functionId}${function.path} - (sync: false, sdk: ${function.useSdk})")
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

        logg.d("server_function", "Call: ${function.functionId}${function.path} - (sync: false, sdk: ${function.useSdk})")

        if (!function.useSdk) {
            try {
                return executeFunction(function, data)
            } catch (e: Exception) {
                if (e is UnknownHostException) {
                    delay(200)
                    return executeFunction(function, data)
                } else {
                    throw e
                }
            }
        }

        return remoteModule.execute {
            val execution = functions.createExecution(
                functionId = function.functionId,
                body = data,
                async = false,
                path = function.path,
                method = ExecutionMethod.entries.first { it.value == function.method },
                headers = mutableMapOf(clientHeader)
            )

            logg.d("server_function", "Response: ${execution.responseStatusCode} ${function.functionId}${function.path} - (sync: false, sdk: ${function.useSdk})")

            FunctionExecutionRaw(
                id = execution.id,
                functionId = execution.functionId,
                status = execution.status.value,
                statusCode = execution.responseStatusCode.toInt(),
                response = execution.responseBody,
                errors = execution.errors
            )
        }
    }
}