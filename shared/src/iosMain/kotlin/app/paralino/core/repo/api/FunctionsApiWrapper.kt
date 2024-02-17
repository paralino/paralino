package app.paralino.core.repo.api

import app.paralino.core.models.Payload
import app.paralino.core.models.getOrThrow
import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.provider.IDeviceApiProvider

class FunctionsApiWrapper(
    private val api: IFunctionsApiIOS
) : IFunctionsApi {
    override suspend fun runAsync(function: ServerFunction, data: String?): String {
        return api.runAsync(function, data).getOrThrow()
    }

    override suspend fun runAndWait(function: ServerFunction, data: String?): FunctionExecutionRaw {
        return api.runAndWait(function, data).getOrThrow()
    }
}

abstract class IFunctionsApiIOS(deviceApiProvider: IDeviceApiProvider) : BaseFunctionsApi(deviceApiProvider) {
    abstract suspend fun runAsync(function: ServerFunction, data: String?): Payload<String>
    abstract suspend fun runAndWait(
        function: ServerFunction,
        data: String?
    ): Payload<FunctionExecutionRaw>
}