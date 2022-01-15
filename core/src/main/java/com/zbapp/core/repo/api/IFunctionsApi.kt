package com.zbapp.core.repo.api

import com.zbapp.core.models.raw.FunctionExecutionRaw

interface IFunctionsApi {
    suspend fun createExecution(functionId: String, data: String? = null): String
    suspend fun getExecution(functionId: String, executionId: String): FunctionExecutionRaw
    suspend fun runAndWait(functionId: String, data: String? = null): FunctionExecutionRaw
}