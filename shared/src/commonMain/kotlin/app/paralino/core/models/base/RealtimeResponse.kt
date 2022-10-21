package app.paralino.core.models.base

data class RealtimeResponse<T>(
    val events: Collection<String>,
    val channels: Collection<String>,
    val timestamp: String,
    var payload: T
)