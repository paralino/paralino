package app.paralino.core.repo.api

import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.Payload
import app.paralino.core.models.getOrThrow

class DocumentApiWrapper(
    private val api: IDocumentApiIOS
) : IDocumentsApi {

    override suspend fun createDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String?,
        data: Any
    ): Map<String, Any> {
        return api.createDocument(databaseType, collectionId, docId, data).getOrThrow()
    }

    override suspend fun updateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String,
        data: Any
    ): Map<String, Any> {
        return api.updateDocument(databaseType, collectionId, docId, data).getOrThrow()
    }

    override suspend fun getDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String
    ): Map<String, Any> {
        return api.getDocument(databaseType, collectionId, documentId).getOrThrow()
    }

    override suspend fun listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        queries: List<BackendQuery>,
        pageSize: Int,
        onNewValues: (List<Map<String, Any>>) -> Boolean
    ) {
        api.listDocuments(databaseType, collectionId, queries, pageSize, onNewValues).getOrThrow()
    }

    override suspend fun deleteDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String
    ) {
        api.deleteDocument(databaseType, collectionId, documentId).getOrThrow()
    }
}

interface IDocumentApiIOS {
    suspend fun createDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String?,
        data: Any
    ): Payload<Map<String, Any>>

    suspend fun updateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String,
        data: Any
    ): Payload<Map<String, Any>>

    suspend fun getDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String
    ): Payload<Map<String, Any>>

    suspend fun listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        queries: List<BackendQuery>,
        pageSize: Int,
        onNewValues: (List<Map<String, Any>>) -> Boolean
    ): Payload<Unit>

    suspend fun deleteDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String
    ): Payload<Unit>
}