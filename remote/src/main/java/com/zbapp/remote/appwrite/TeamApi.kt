package com.zbapp.remote.appwrite

import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.api.ITeamApi
import com.zbapp.core.repo.api.TeamListResponse
import com.zbapp.core.repo.api.TeamMembershipsResponse
import com.zbapp.core.repo.api.TeamResponse
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

// TODO parse properly
class TeamApi constructor(
    private val remoteModule: AppWriteRemoteModule,
) : ITeamApi {

    override suspend fun createOrUpdateTeam(data: GroupDocumentRaw): TeamResponse {
        return if (data.id.isNotBlank()) updateTeam(data)
        else createTeam(data)
    }

    private suspend fun createTeam(data: GroupDocumentRaw): TeamResponse {
        return remoteModule.execute {
            teams.create(uniqueIdPlaceholder, BaseRemoteModule.formatter.encodeToString(data)).toString()
        }
    }

    private suspend fun updateTeam(data: GroupDocumentRaw): TeamResponse {
        return remoteModule.execute {
            teams.update(data.id, BaseRemoteModule.formatter.encodeToString(data)).toString()
        }
    }

    override suspend fun deleteTeam(teamId: String) {
        remoteModule.execute { teams.delete(teamId) }
    }

    override suspend fun removeMember(teamId: String, membershipId: String) {
        remoteModule.execute { teams.deleteMembership(teamId, membershipId) }
    }

    override suspend fun getTeam(teamId: String): TeamResponse {
        return remoteModule.execute { teams.get(teamId).toString() }
    }

    override suspend fun getTeams(): TeamListResponse {
        return remoteModule.execute { teams.list().toString() }
    }

    override suspend fun getTeamMembers(teamId: String): TeamMembershipsResponse {
        return remoteModule.execute { teams.getMemberships(teamId).toString() }
    }

}