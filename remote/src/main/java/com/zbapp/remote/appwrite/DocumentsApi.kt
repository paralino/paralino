package com.zbapp.remote.appwrite

import com.zbapp.core.repo.group.DocumentResponse
import com.zbapp.core.repo.group.DocumentsResponse
import com.zbapp.core.repo.group.IDocumentsApi
import kotlinx.serialization.json.JsonObject

class DocumentsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IDocumentsApi {

    override suspend fun createOrUpdateDocument(
        collectionId: String,
        groupId: String?,
        data: JsonObject,
        users: List<String>?
    ): DocumentResponse {
        return remoteModule.execute {
            if (groupId.isNullOrBlank())
                database.createDocument(collectionId, data, read = users).body!!.string()
            else
                database.updateDocument(collectionId, groupId, data, read = users).body!!.string()
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

