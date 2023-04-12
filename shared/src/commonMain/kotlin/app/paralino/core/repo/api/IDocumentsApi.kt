package app.paralino.core.repo.api

import app.paralino.core.models.BackendDatabaseType

abstract class IDocumentsApi {

    abstract suspend fun createDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String? = null,
        data: Any
    ): Map<String, Any>

    abstract suspend fun updateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String,
        data: Any
    ): Map<String, Any>

    abstract suspend fun getDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String): Map<String, Any>

    suspend fun listAllDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        keyValues: List<Pair<String, Any>> = emptyList(),
    ): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        listDocuments(
            databaseType = databaseType,
            collectionId = collectionId,
            keyValues = keyValues,
            limit = 50,
            onNewValues = {
                result.addAll(it)
                true
            },
        )
        return result.toList()
    }

    abstract suspend fun listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        keyValues: List<Pair<String, Any>> = emptyList(),
        limit: Int = 50,
        onNewValues: (List<Map<String, Any>>) -> Boolean,
    )

    abstract suspend fun deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String)

}