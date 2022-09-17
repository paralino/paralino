package app.paralino.core.repo.api

import app.paralino.core.models.BackendDatabaseType

interface IDocumentsApi {

    suspend fun createOrUpdateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String? = null,
        data: Any
    ): Map<String, Any>

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

    suspend fun getDocuments(databaseType: BackendDatabaseType, collectionId: String): List<Map<String, Any>>
    suspend fun getDocumentsFor(databaseType: BackendDatabaseType, collectionId: String, vararg keyValue: Pair<String, Any>): List<Map<String, Any>>

    suspend fun deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String)

}