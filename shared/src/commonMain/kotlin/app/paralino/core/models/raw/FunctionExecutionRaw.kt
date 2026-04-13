package app.paralino.core.models.raw

import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.fromRemote
import app.paralino.core.utils.extensions.fromJsonObjectToMapOfPrimitives
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

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
                BaseRemoteModule.formatter.decodeFromString<FunctionResponse<FunctionErrorBody>>(response).data
            } catch (e: Exception) {
                null
            }
        }

    val remoteException: BaseRemoteException get() {
        return BaseRemoteException.fromRemote(
            code = statusCode,
            error = errorResponse
        )
    }

    val responseData: JsonElement?
        get() {
            return BaseRemoteModule.formatter.parseToJsonElement(response).jsonObject.get("data")
        }

    val responseDataMap: Map<String, Any>?
        get() {
            return response.fromJsonObjectToMapOfPrimitives()
                .filterValues { it != null }
                .mapValues { it.value!! }
                .get("data") as? Map<String, Any>
        }

    inline fun <reified T> response(): FunctionResponse<T> {
        return BaseRemoteModule.formatter.decodeFromString(response)
    }
}

@Serializable
data class FunctionResponse<T>(
    val data: T
)

@Serializable
data class FunctionErrorBody(
    val type: String = "general_error",
    val message: String,
    val code: Int
)