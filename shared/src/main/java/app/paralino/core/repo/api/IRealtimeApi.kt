package app.paralino.core.repo.api

import kotlinx.coroutines.flow.Flow

interface IRealtimeApi {
    fun subscribe(vararg channels: String): Flow<Map<String, Any>>
    fun subscribeWithFilter(channel: String, vararg filter: String): Flow<Map<String, Any>>
    fun subscribeWithFilter(channel: String, filters: List<String> = listOf()): Flow<Map<String, Any>>
    fun subscribeWithFilter(channels: List<String>, filters: List<String> = listOf()): Flow<Map<String, Any>>
}