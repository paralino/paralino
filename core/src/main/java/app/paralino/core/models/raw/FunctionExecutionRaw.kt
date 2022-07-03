package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FunctionExecutionRaw constructor(
    @SerialName("\$id")
    val id: String,
    val functionId: String,
    val status: String,
    val statusCode: Long,
    val response: String,
    val stderr: String
) {

    val isSuccess: Boolean get() = status == "completed"
    val isFail: Boolean get() = status == "failed"
    val isCompleted: Boolean get() = isSuccess || isFail
}
