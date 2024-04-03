package app.paralino.core.repo.api

import app.paralino.core.models.base.RealtimeResponse
import kotlinx.coroutines.flow.Flow

abstract class IRealtimeApi {
    fun subscribe(vararg channels: String): Flow<RealtimeResponse<Map<String, Any>>> {
        return subscribeWithFilter(channels.toList(), listOf())
    }

    fun subscribeWithFilter(
        channel: String,
        vararg filter: String
    ): Flow<RealtimeResponse<Map<String, Any>>> {
        return subscribeWithFilter(listOf(channel), filter.toList())
    }

    fun subscribeWithFilter(
        channel: String,
        filters: List<String>
    ): Flow<RealtimeResponse<Map<String, Any>>> {
        return subscribeWithFilter(listOf(channel), filters)
    }

    abstract fun subscribeWithFilter(
        channels: List<String>,
        filters: List<String> = listOf()
    ): Flow<RealtimeResponse<Map<String, Any>>>
}