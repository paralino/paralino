package app.paralino.core.repo.api

import kotlinx.coroutines.flow.Flow

interface IRealtimeApi {
    fun subscribe(vararg channels: String): Flow<Map<String, Any>>
}