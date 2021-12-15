package com.zbapp.core.repo.group

import kotlinx.serialization.json.JsonObject

interface IDocumentsApi {

    suspend fun createChildDocument(
        collectionId: String,
        data: Any,
        read: List<String>?,
        write: List<String>?,
        parentDocId: String,
        parentDocProperty: String,
        appendToParent: Boolean
    ): DocumentResponse

    suspend fun createOrUpdateDocument(
        collectionId: String,
        data: Any,
        docId: String? = null,
        read: List<String>? = null,
        write: List<String>? = null
    ): DocumentResponse

    suspend fun getDocument(collectionId: String, groupId: String): DocumentResponse

    suspend fun getDocuments(collectionId: String): DocumentsResponse

    suspend fun deleteDocument(collectionId: String, groupId: String)

}

typealias DocumentsResponse = String
typealias DocumentResponse = String