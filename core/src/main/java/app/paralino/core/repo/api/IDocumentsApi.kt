package app.paralino.core.repo.api

import app.paralino.core.models.BackendDatabaseType

interface IDocumentsApi {

    suspend fun createOrUpdateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String? = null,
        data: Any,
        read: List<String>? = null,
        write: List<String>? = null
    ): Map<String, Any>

    suspend fun getDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String): Map<String, Any>

    suspend fun getDocuments(databaseType: BackendDatabaseType, collectionId: String): List<Map<String, Any>>
    suspend fun getDocumentsFor(databaseType: BackendDatabaseType, collectionId: String, vararg keyValue: Pair<String, Any>): List<Map<String, Any>>

    suspend fun deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String)

}