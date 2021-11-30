package com.zbapp.core.models.raw

import com.zbapp.core.repo.group.DocumentModel
import kotlinx.serialization.Serializable

@Serializable
data class MemberDocumentRaw constructor(
    val id: String,
    val ownerId: String?,
    val data: String,
    val mutableData: String? = null
) {
    companion object {
        fun fromRemote(data: DocumentModel): MemberDocumentRaw {
            return MemberDocumentRaw(
                id = data["id"] ?: "",
                ownerId = data["ownerId"] ?: "",
                data = data["data"] ?: "",
                mutableData = data["mutableData"] ?: ""
            )
        }
    }
}