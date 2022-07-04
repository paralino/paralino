package app.paralino.remote.appwrite

import app.paralino.core.repo.api.IDocumentsApi
import io.appwrite.Query
import io.appwrite.models.Document
import io.appwrite.models.DocumentList

class DocumentsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IDocumentsApi {

    override suspend fun createOrUpdateDocument(
        collectionId: String,
        docId: String?,
        data: Any,
        read: List<String>?,
        write: List<String>?
    ): Map<String, Any> {
        return remoteModule.execute {
            if (docId.isNullOrBlank())
                database.createDocument(
                    collectionId = collectionId,
                    documentId = uniqueIdPlaceholder,
                    data = data,
                    read = read,
                    write = write
                ).parseDocument()
            else
                database.updateDocument(
                    collectionId = collectionId,
                    documentId = docId,
                    data = data,
                    read = read,
                    write = write
                ).parseDocument()
        }
    }

    override suspend fun getDocument(collectionId: String, documentId: String): Map<String, Any> {
        return remoteModule.execute { database.getDocument(collectionId, documentId).parseDocument() }
    }

    override suspend fun getDocuments(collectionId: String): List<Map<String, Any>> {
        return remoteModule.execute { database.listDocuments(collectionId).parseListOfDocuments() }
    }

    override suspend fun getDocumentsFor(collectionId: String, vararg keyValue: Pair<String, Any>): List<Map<String, Any>> {
        return remoteModule.execute {
            database.listDocuments(
                collectionId = collectionId,
                queries = keyValue.map { Query.equal(attribute = it.first, value = it.second) }
            ).parseListOfDocuments()
        }
    }

    override suspend fun deleteDocument(collectionId: String, documentId: String) {
        remoteModule.execute { database.deleteDocument(collectionId, documentId) }
    }

    private fun Document.parseDocument(): Map<String, Any> {
        return this.data
    }

    private fun DocumentList.parseListOfDocuments(): List<Map<String, Any>> {
        return this.documents.map { it.parseDocument() }
    }

}

