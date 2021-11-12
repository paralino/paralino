package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class SimpleDocumentRaw(
    val id: String,
    val ownerId: String?,
    val data: String
)