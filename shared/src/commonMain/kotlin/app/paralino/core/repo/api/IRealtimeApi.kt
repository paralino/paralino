package app.paralino.core.repo.api

import app.paralino.core.models.base.RealtimeResponse
import kotlinx.coroutines.flow.Flow

interface IRealtimeApi {
    fun subscribe(vararg channels: String): Flow<RealtimeResponse<Map<String, Any>>>
    fun subscribeWithFilter(channel: String, vararg filter: String): Flow<RealtimeResponse<Map<String, Any>>>
    fun subscribeWithFilter(channel: String, filters: List<String> = listOf()): Flow<RealtimeResponse<Map<String, Any>>>
    fun subscribeWithFilter(channels: List<String>, filters: List<String> = listOf()): Flow<RealtimeResponse<Map<String, Any>>>
}