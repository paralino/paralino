package app.paralino.core.repo.api

import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

abstract class BaseFunctionsApi(
    private val deviceApiProvider: IDeviceApiProvider
) {
    private val clientHeaderValue: String =
        "${deviceApiProvider.deviceType.key}/${deviceApiProvider.appVersionCode}/${deviceApiProvider.appVersion}"
    val clientHeader: Pair<String, String> = "x-paralino-client" to clientHeaderValue
}

interface IFunctionsApi {
    suspend fun runAsync(function: ServerFunction, data: String? = null): String
    suspend fun runAndWait(function: ServerFunction, data: String? = null): FunctionExecutionRaw
}

suspend inline fun <reified T> IFunctionsApi.runAndWait(
    function: ServerFunction,
    data: T
): FunctionExecutionRaw {
    return runAndWait(
        function = function,
        data = BaseRemoteModule.formatter.encodeToString(data)
    )
}