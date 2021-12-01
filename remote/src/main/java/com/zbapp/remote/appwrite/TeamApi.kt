package com.zbapp.remote.appwrite

import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.group.ITeamApi
import com.zbapp.core.repo.group.TeamListResponse
import com.zbapp.core.repo.group.TeamMembershipsResponse
import com.zbapp.core.repo.group.TeamResponse
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

class TeamApi constructor(
    private val remoteModule: AppWriteRemoteModule,
) : ITeamApi {

    override suspend fun createOrUpdateTeam(data: GroupDocumentRaw): TeamResponse {
        return if (data.id.isNotBlank()) updateTeam(data)
        else createTeam(data)
    }

    private suspend fun createTeam(data: GroupDocumentRaw): TeamResponse {
        return remoteModule.execute {
            teams.create(BaseRemoteModule.formatter.encodeToString(data)).body!!.string()
        }
    }

    private suspend fun updateTeam(data: GroupDocumentRaw): TeamResponse {
        return remoteModule.execute {
            teams.update(data.id, BaseRemoteModule.formatter.encodeToString(data)).body!!.string()
        }
    }

    override suspend fun deleteTeam(teamId: String) {
        remoteModule.execute { teams.delete(teamId) }
    }

    override suspend fun removeMember(teamId: String, membershipId: String) {
        remoteModule.execute { teams.deleteMembership(teamId, membershipId) }
    }

    override suspend fun getTeam(teamId: String): TeamResponse {
        return remoteModule.execute { teams.get(teamId).body!!.string() }
    }

    override suspend fun getTeams(): TeamListResponse {
        return remoteModule.execute { teams.list().body!!.string() }
    }

    override suspend fun getTeamMembers(teamId: String): TeamMembershipsResponse {
        return remoteModule.execute { teams.getMemberships(teamId).body!!.string() }
    }

}