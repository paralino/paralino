package com.zbapp.core.repo.api

interface IFunctionsApi {
    suspend fun createExecution(functionId: String, data: String? = null): String
}