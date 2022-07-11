package app.paralino.remote.appwrite

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
        data: Any,
        read: List<String>?,
        write: List<String>?
    ): Map<String, Any> {
        return remoteModule.execute {
            if (docId.isNullOrBlank())
                databaseForType(databaseType).createDocument(
                    collectionId = collectionId,
                    documentId = uniqueIdPlaceholder,
                    data = data,
                    read = read,
                    write = write
                ).parseDocument()
            else
                databaseForType(databaseType).updateDocument(
                    collectionId = collectionId,
                    documentId = docId,
                    data = data,
                    read = read,
                    write = write
                ).parseDocument()
        }
    }

    override suspend fun getDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String,
    ): Map<String, Any> {
        return remoteModule.execute { databaseForType(databaseType).getDocument(collectionId, documentId).parseDocument() }
    }

    override suspend fun getDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
    ): List<Map<String, Any>> {
        return remoteModule.execute { databaseForType(databaseType).listDocuments(collectionId).parseListOfDocuments() }
    }

    override suspend fun getDocumentsFor(
        databaseType: BackendDatabaseType,
        collectionId: String,
        vararg keyValue: Pair<String, Any>,
    ): List<Map<String, Any>> {
        return remoteModule.execute {
            databaseForType(databaseType).listDocuments(
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
        remoteModule.execute { databaseForType(databaseType).deleteDocument(collectionId, documentId) }
    }

    private fun Document.parseDocument(): Map<String, Any> {
        return this.data
    }

    private fun DocumentList.parseListOfDocuments(): List<Map<String, Any>> {
        return this.documents.map { it.parseDocument() }
    }

}

