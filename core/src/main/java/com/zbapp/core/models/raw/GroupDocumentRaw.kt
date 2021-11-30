package com.zbapp.core.models.raw

import com.zbapp.core.repo.group.DocumentModel
import kotlinx.serialization.Serializable

@Serializable
data class GroupDocumentRaw constructor(
    val id: String,
    val ownerId: String?,
    val data: String
) {
    companion object {
        fun fromRemote(data: DocumentModel): GroupDocumentRaw {
            return GroupDocumentRaw(
                id = data["id"] ?: "",
                ownerId = data["ownerId"] ?: "",
                data = data["data"] ?: ""
            )
        }
    }
}