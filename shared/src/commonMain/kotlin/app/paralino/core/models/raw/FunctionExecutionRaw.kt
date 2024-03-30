package app.paralino.core.models.raw

import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.fromJsonObjectToMapOfPrimitives
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FunctionExecutionRaw constructor(
    @SerialName("\$id")
    val id: String,
    val functionId: String,
    val status: String,
    val statusCode: Int,
    val response: String,
    val errors: String
) {

    val isSuccess: Boolean get() = status == "completed" && statusCode in 200..299
    val isFail: Boolean get() = status == "failed"
    val isCompleted: Boolean get() = isSuccess || isFail
    val stderr: String = ""

    val errorResponse: FunctionErrorBody?
        get() {
            return try {
                BaseRemoteModule.formatter.decodeFromString(response)
            } catch (e: Exception) {
                null
            }
        }

    val responseDataMap: Map<String, Any>?
        get() {
            return response.fromJsonObjectToMapOfPrimitives()
                .filterValues { it != null }
                .mapValues { it.value!! }
                .get("data") as? Map<String, Any>
        }
}

@Serializable
data class FunctionErrorBody(
    val type: String = "general_error",
    val message: String,
    val code: Int
)