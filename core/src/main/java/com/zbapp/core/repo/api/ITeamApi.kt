package com.zbapp.core.repo.api

import com.zbapp.core.models.raw.GroupDocumentRaw

interface ITeamApi {
    suspend fun createOrUpdateTeam(data: GroupDocumentRaw): TeamResponse
    suspend fun deleteTeam(teamId: String)
    suspend fun removeMember(teamId: String, membershipId: String)
    suspend fun getTeam(teamId: String): TeamResponse
    suspend fun getTeams(): TeamListResponse
    suspend fun getTeamMembers(teamId: String): TeamMembershipsResponse
}

typealias TeamResponse = String
typealias TeamListResponse = String
typealias TeamMembershipsResponse = String