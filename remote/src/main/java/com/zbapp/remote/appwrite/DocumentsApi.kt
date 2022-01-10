package com.zbapp.remote.appwrite

import com.zbapp.core.repo.api.IDocumentsApi
import io.appwrite.models.Document
import io.appwrite.models.DocumentList

class DocumentsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IDocumentsApi {

    override suspend fun createOrUpdateDocument(
        collectionId: String,
        data: Any,
        docId: String?,
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

    override suspend fun getDocument(collectionId: String, groupId: String): Map<String, Any> {
        return remoteModule.execute { database.getDocument(collectionId, groupId).parseDocument() }
    }

    override suspend fun getDocuments(collectionId: String): List<Map<String, Any>> {
        return remoteModule.execute { database.listDocuments(collectionId).parseListOfDocuments() }
    }

    override suspend fun deleteDocument(collectionId: String, groupId: String) {
        remoteModule.execute { database.deleteDocument(collectionId, groupId) }
    }

    private fun Document.parseDocument(): Map<String, Any> {
        return this.data
    }

    private fun DocumentList.parseListOfDocuments(): List<Map<String, Any>> {
        return this.documents.map { it.parseDocument() }
    }

}

