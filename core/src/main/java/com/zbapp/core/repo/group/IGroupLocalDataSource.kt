package com.zbapp.core.repo.group

import com.zbapp.core.models.raw.SimpleDocumentRaw
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun replaceGroups(data: List<SimpleDocumentRaw>)
    suspend fun updateOrCreateGroup(group: SimpleDocumentRaw)

    fun getGroup(groupId: String): Flow<SimpleDocumentRaw>
    fun getAllGroups(): Flow<List<SimpleDocumentRaw>>

    suspend fun deleteGroup(groupId: String)
    suspend fun clearAll()

}