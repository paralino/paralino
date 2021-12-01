package com.zbapp.core.repo.group

import com.zbapp.core.models.raw.GroupDocumentRaw
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun recreateGroups(data: List<GroupDocumentRaw>)
    suspend fun updateOrCreateGroup(group: GroupDocumentRaw)

    fun getGroup(groupId: String): Flow<GroupDocumentRaw>
    fun getAllGroups(): Flow<List<GroupDocumentRaw>>

    suspend fun deleteGroup(groupId: String)
    suspend fun clearAll()

}