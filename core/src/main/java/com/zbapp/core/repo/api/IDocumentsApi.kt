package com.zbapp.core.repo.api

interface IDocumentsApi {

    suspend fun createOrUpdateDocument(
        collectionId: String,
        data: Any,
        docId: String? = null,
        read: List<String>? = null,
        write: List<String>? = null
    ): Map<String, Any>

    suspend fun getDocument(collectionId: String, groupId: String): Map<String, Any>

    suspend fun getDocuments(collectionId: String): List<Map<String, Any>>

    suspend fun deleteDocument(collectionId: String, groupId: String)

}