package app.paralino.core.repo.api

import app.paralino.core.BuildKonfig
import app.paralino.core.logg
import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.utils.BaseRemoteModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString

abstract class BaseFunctionsApi(
    private val deviceApiProvider: IDeviceApiProvider
) {
    private val clientHeaderValue: String =
        "${deviceApiProvider.deviceType.key}/${deviceApiProvider.appVersionCode}/${deviceApiProvider.appVersion}"
    val clientHeader: Pair<String, String> = "x-paralino-client" to clientHeaderValue

    suspend fun executeFunction(function: ServerFunction, data: String?): FunctionExecutionRaw {
        val client = HttpClient() {
            if (BuildKonfig.isDevMode) {
                install(Logging) {
                    logger = object : io.ktor.client.plugins.logging.Logger {
                        override fun log(message: String) {
                            logg.d("HTTP_KTOR", message)
                        }
                    }
                    level = LogLevel.INFO
                }
            }
        }
        val response = client.request(urlString = function.functionId + function.path) {
            method = HttpMethod.parse(function.method)
            headers {
                if (data != null) {
                    contentType(ContentType.Text.Plain)
                }
                append(clientHeader.first, clientHeader.second)
            }
            if (data != null) {
                setBody(data)
            }
        }
        return FunctionExecutionRaw(
            id = "",
            functionId = function.functionId + function.path,
            status = "completed",
            statusCode = response.status.value,
            response = response.bodyAsText(),
            errors = ""
        )
    }
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

suspend inline fun <reified T> IFunctionsApi.runAsync(
    function: ServerFunction,
    data: T
): String {
    return runAsync(
        function = function,
        data = BaseRemoteModule.formatter.encodeToString(data)
    )
}