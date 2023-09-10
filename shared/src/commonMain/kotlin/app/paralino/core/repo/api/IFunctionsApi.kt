package app.paralino.core.repo.api

import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.models.raw.GenericFunctionPayload
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

interface IFunctionsApi {
    suspend fun createExecution(functionId: String, data: String? = null): String
    suspend fun runAndWait(functionId: String, data: String? = null): FunctionExecutionRaw
}

suspend inline fun < reified T> IFunctionsApi.runAndWait(functionId: String, data: GenericFunctionPayload<T>? = null): FunctionExecutionRaw {
    return runAndWait(
        functionId = functionId,
        data = data?.let { BaseRemoteModule.formatter.encodeToString(it) }
    )
}