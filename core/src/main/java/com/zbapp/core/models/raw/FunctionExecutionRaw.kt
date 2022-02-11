package com.zbapp.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FunctionExecutionRaw(
    @SerialName("\$id")
    val id: String,
    val functionId: String,
    val status: String,
    val stdout: String,
    val stderr: String
) {

    val isSuccess: Boolean get() = status == "completed"
    val isFail: Boolean get() = status == "failed"
    val isCompleted: Boolean get() = isSuccess || isFail

    companion object {
        fun from(map: Map<String, Any>): FunctionExecutionRaw {
            return FunctionExecutionRaw (
                id = map["\$id"] as String,
                functionId = map["functionId"] as String,
                status = map["status"] as String,
                stdout = map["stdout"] as String,
                stderr = map["stderr"] as String
            )
        }
    }
}
