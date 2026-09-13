package app.paralino.core.repo.api

import app.paralino.core.models.base.BackendDatabaseType

interface IDocumentsApi {

    suspend fun createDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String? = null,
        data: Any
    ): Map<String, Any>

    suspend fun updateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String,
        data: Any
    ): Map<String, Any>

    suspend fun getDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String): Map<String, Any>

    suspend fun listAllDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        queries: List<BackendQuery> = emptyList(),
    ): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        listDocuments(
            databaseType = databaseType,
            collectionId = collectionId,
            queries = queries,
            limit = 50,
            onNewValues = {
                result.addAll(it)
                true
            },
        )
        return result.toList()
    }

    suspend fun listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        queries: List<BackendQuery> = emptyList(),
        limit: Int = 50,
        onNewValues: (List<Map<String, Any>>) -> Boolean,
    )

    suspend fun deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String)
}

sealed interface BackendQuery {
    data class OR(val queries: List<BackendQuery>): BackendQuery
    data class Equals(val key: String, val value: Any): BackendQuery
    data class IsNull(val key: String): BackendQuery
    data class GreaterThan(val key: String, val value: Any): BackendQuery
    data class UpdatedAfter(val value: String): BackendQuery
}