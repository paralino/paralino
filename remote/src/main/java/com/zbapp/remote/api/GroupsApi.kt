package com.zbapp.remote.api

import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.repo.group.GroupResponse
import com.zbapp.core.repo.group.GroupsResponse
import com.zbapp.core.repo.group.IGroupsApi
import com.zbapp.remote.appwrite.AppWriteRemoteModule

class GroupsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IGroupsApi {

    override suspend fun createOrUpdateGroup(collectionId: String, data: SimpleDocumentRaw, users: List<String>?): GroupResponse {
        return createOrUpdateGroup(collectionId, data.id, mapOf("data" to data.data), users)
    }

    override suspend fun createOrUpdateGroup(collectionId: String, groupId: String, data: Map<String, String>, users: List<String>?): GroupResponse {
        return remoteModule.execute {
            if(groupId.isBlank())
                database.createDocument(collectionId, data, read = users).body!!.string()
            else
                database.updateDocument(collectionId, groupId, data, read = users).body!!.string()
        }
    }

    override suspend fun getGroup(collectionId: String, groupId: String): GroupResponse {
        return remoteModule.execute { database.getDocument(collectionId, groupId).body!!.string() }
    }

    override suspend fun getGroups(collectionId: String): GroupsResponse {
        return remoteModule.execute { database.listDocuments(collectionId).body!!.string() }
    }

    override suspend fun deleteGroup(collectionId: String, groupId: String) {
        remoteModule.execute { database.deleteDocument(collectionId, groupId) }
    }

}

