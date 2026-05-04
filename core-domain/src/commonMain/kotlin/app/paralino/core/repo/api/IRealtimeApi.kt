package app.paralino.core.repo.api

import app.paralino.core.models.base.RealtimeResponse
import kotlinx.coroutines.flow.Flow

abstract class IRealtimeApi {
    abstract fun subscribeToChannels(
        channels: List<String>
    ): Flow<RealtimeResponse<Map<String, Any>>>
}