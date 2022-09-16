package app.paralino.android.remote.appwrite

import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.repo.api.IDocumentsApi
import io.appwrite.Query
import io.appwrite.models.Document
import io.appwrite.models.DocumentList

class DocumentsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IDocumentsApi {

    override suspend fun createOrUpdateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String?,
        data: Any
    ): Map<String, Any> {
        if (docId.isNullOrBlank()) {
            return createDocument(
                databaseType = databaseType,
                collectionId = collectionId,
                docId = docId,
                data = data
            )
        } else {
            return updateDocument(
                databaseType = databaseType,
                collectionId = collectionId,
                docId = docId,
                data = data
            )
        }
    }

    override suspend fun createDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String?,
        data: Any
    ): Map<String, Any> {
        return remoteModule.execute {
            database.createDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = docId ?: uniqueIdPlaceholder,
                data = data
            ).parseDocument()
        }
    }

    override suspend fun updateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String,
        data: Any
    ): Map<String, Any> {
        return remoteModule.execute {
            database.updateDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = docId,
                data = data
            ).parseDocument()
        }
    }

    override suspend fun getDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String,
    ): Map<String, Any> {
        return remoteModule.execute {
            database.getDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = documentId
            ).parseDocument()
        }
    }

    override suspend fun getDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
    ): List<Map<String, Any>> {
        return remoteModule.execute {
            database.listDocuments(databaseId = databaseType.id, collectionId = collectionId)
                .parseListOfDocuments()
        }
    }

    override suspend fun getDocumentsFor(
        databaseType: BackendDatabaseType,
        collectionId: String,
        vararg keyValue: Pair<String, Any>,
    ): List<Map<String, Any>> {
        return remoteModule.execute {
            database.listDocuments(
                databaseId = databaseType.id,
                collectionId = collectionId,
                queries = keyValue.map { Query.equal(attribute = it.first, value = it.second) }
            ).parseListOfDocuments()
        }
    }

    override suspend fun deleteDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String,
    ) {
        remoteModule.execute {
            database.deleteDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = documentId
            )
        }
    }

    private fun Document.parseDocument(): Map<String, Any> {
        return this.data
    }

    private fun DocumentList.parseListOfDocuments(): List<Map<String, Any>> {
        return this.documents.map { it.parseDocument() }
    }

}

