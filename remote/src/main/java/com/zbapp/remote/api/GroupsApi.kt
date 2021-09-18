package com.zbapp.remote.api

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.remote.appwrite.AppWriteRemoteModule

class GroupsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) {

    suspend fun getMyLocationsHistory(collectionId: String, date: String): List<BaseLocationModel> {
        remoteModule.database.listDocuments(
            collectionId,
            search = date
        )
    }

}