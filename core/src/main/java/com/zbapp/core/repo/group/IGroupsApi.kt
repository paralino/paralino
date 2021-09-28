package com.zbapp.core.repo.group

import com.zbapp.core.models.raw.SimpleDocumentRaw

interface IGroupsApi {
    suspend fun createOrUpdateGroup(
        collectionId: String,
        data: SimpleDocumentRaw,
        users: List<String>? = null
    ): GroupResponse

    suspend fun createOrUpdateGroup(
        collectionId: String,
        groupId: String,
        data: Map<String, String>,
        users: List<String>? = null
    ): GroupResponse

    suspend fun getGroup(collectionId: String, groupId: String): GroupResponse

    suspend fun getGroups(collectionId: String): GroupsResponse

    suspend fun deleteGroup(collectionId: String, groupId: String)
}

typealias GroupsResponse = String
typealias GroupResponse = String