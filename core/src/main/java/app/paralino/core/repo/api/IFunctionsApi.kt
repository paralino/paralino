package app.paralino.core.repo.api

import app.paralino.core.models.raw.FunctionExecutionRaw

interface IFunctionsApi {
    suspend fun createExecution(functionId: String, data: String? = null): String
    suspend fun getExecution(functionId: String, executionId: String): FunctionExecutionRaw
    suspend fun runAndWait(functionId: String, data: String? = null): FunctionExecutionRaw
}