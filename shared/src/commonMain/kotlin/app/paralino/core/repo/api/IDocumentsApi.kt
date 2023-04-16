package app.paralino.core.repo.api

import app.paralino.core.models.BackendDatabaseType

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
        keyValues: List<Pair<String, Any>> = emptyList(),
    ): List<Map<String, Any>>

    suspend fun listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        keyValues: List<Pair<String, Any>> = emptyList(),
        limit: Int = 50,
        onNewValues: (List<Map<String, Any>>) -> Boolean,
    )

    suspend fun deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String)

}