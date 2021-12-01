package com.zbapp.core.repo.group

import kotlinx.serialization.json.JsonObject

interface IDocumentsApi {

    suspend fun createOrUpdateDocument(
        collectionId: String,
        groupId: String?,
        data: JsonObject,
        users: List<String>? = null
    ): DocumentResponse

    suspend fun getDocument(collectionId: String, groupId: String): DocumentResponse

    suspend fun getDocuments(collectionId: String): DocumentsResponse

    suspend fun deleteDocument(collectionId: String, groupId: String)

}

typealias DocumentsResponse = String
typealias DocumentResponse = String