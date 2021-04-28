package com.zbapp.remote

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.location.ILocationRemoteDataSource
import com.zbapp.remote.api.GroupsApi
import com.zbapp.remote.user.RemoteUserCache

class LocationRemoteSource internal constructor(
    private val api: GroupsApi
): ILocationRemoteDataSource {

    constructor(user: RemoteUserCache) : this(GroupsApi(user))

    override suspend fun getCurrentLocationsForGroup(groupId: String): List<BaseLocationModel> {
        try { return api.getCurrentLocationsForGroup(groupId) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun getLocationsHistoryForUser(groupId: String, userId: String): List<BaseLocationModel> {
        try { return api.getLocationsHistoryForUser(groupId, userId) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun getMyLocationsHistory(date: String): List<BaseLocationModel> {
        try { return api.getMyLocationsHistory(date) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun setLocations(current: List<BaseLocationModel>, history: List<BaseLocationModel>) {
        try { api.setLocations(current, history) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

}