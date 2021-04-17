package com.zbapp.remote

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.location.ILocationRemoteDataSource
import com.zbapp.remote.api.GroupsApi
import com.zbapp.remote.api.UserApi
import com.zbapp.remote.user.RemoteUserCache

class GroupsRemoteSource internal constructor(
    private val api: GroupsApi
): ILocationRemoteDataSource {

    constructor(user: RemoteUserCache) : this(GroupsApi(user))

    override suspend fun getCurrentLocationsForCircle(circleId: String): List<BaseLocationModel> {
        try { return api.getCurrentLocationsForCircle(circleId) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun getLocationsHistoryForUser(circleId: String, userId: String): List<BaseLocationModel> {
        try { return api.getLocationsHistoryForUser(circleId, userId) }
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

    suspend fun initGroupsCollection() {
        try { api.initGroupsCollection() }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

}