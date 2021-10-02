package com.zbapp.core.repo.group

import com.zbapp.core.models.raw.SimpleDocumentRaw

interface IDocumentsApi {
    suspend fun createOrUpdateDocument(
        collectionId: String,
        data: SimpleDocumentRaw,
        users: List<String>? = null
    ): DocumentResponse

    suspend fun createOrUpdateDocument(
        collectionId: String,
        groupId: String,
        data: Map<String, String>,
        users: List<String>? = null
    ): DocumentResponse

    suspend fun getDocument(collectionId: String, groupId: String): DocumentResponse

    suspend fun getDocuments(collectionId: String): DocumentsResponse

    suspend fun deleteDocument(collectionId: String, groupId: String)
}

typealias DocumentsResponse = String
typealias DocumentResponse = String