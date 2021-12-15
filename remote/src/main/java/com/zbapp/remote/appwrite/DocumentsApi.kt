package com.zbapp.remote.appwrite

import com.zbapp.core.repo.group.DocumentResponse
import com.zbapp.core.repo.group.DocumentsResponse
import com.zbapp.core.repo.group.IDocumentsApi
import kotlinx.serialization.json.JsonObject

class DocumentsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IDocumentsApi {

    override suspend fun createChildDocument(
        collectionId: String,
        data: Any,
        read: List<String>?,
        write: List<String>?,
        parentDocId: String,
        parentDocProperty: String,
        appendToParent: Boolean
    ): DocumentResponse {
        return remoteModule.execute {
            database.createDocument(
                collectionId = collectionId,
                data = data,
                read = read,
                write = write,
                parentDocument = parentDocId,
                parentProperty = parentDocProperty,
                parentPropertyType = if(appendToParent) "append" else "assign"
            ).body!!.string()
        }
    }

    override suspend fun createOrUpdateDocument(
        collectionId: String,
        data: Any,
        docId: String?,
        read: List<String>?,
        write: List<String>?
    ): DocumentResponse {
        return remoteModule.execute {
            if (docId.isNullOrBlank())
                database.createDocument(collectionId, data, read = read, write = write).body!!.string()
            else
                database.updateDocument(collectionId, docId, data, read = read, write).body!!.string()
        }
    }

    override suspend fun getDocument(collectionId: String, groupId: String): DocumentResponse {
        return remoteModule.execute { database.getDocument(collectionId, groupId).body!!.string() }
    }

    override suspend fun getDocuments(collectionId: String): DocumentsResponse {
        return remoteModule.execute { database.listDocuments(collectionId).body!!.string() }
    }

    override suspend fun deleteDocument(collectionId: String, groupId: String) {
        remoteModule.execute { database.deleteDocument(collectionId, groupId) }
    }

}

