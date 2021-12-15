package com.zbapp.core.repo.group

import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ITeamApi {
    suspend fun createOrUpdateTeam(data: GroupDocumentRaw): TeamResponse

    suspend fun deleteTeam(teamId: String)

    suspend fun removeMember(teamId: String, membershipId: String)

    suspend fun getTeam(teamId: String): TeamResponse

    suspend fun getTeams(): TeamListResponse

    suspend fun getTeamMembers(teamId: String): TeamMembershipsResponse

    companion object {
        fun getTeamFromJson(content: TeamResponse): GroupDocumentRaw {
            val json = BaseRemoteModule.formatter.parseToJsonElement(content)
            return parseTeam(json)
        }

        fun getTeamsFromJson(content: TeamListResponse): List<GroupDocumentRaw> {
            val json = BaseRemoteModule.formatter.parseToJsonElement(content)
            return json.jsonObject["teams"]?.jsonArray?.map { parseTeam(it) } ?: emptyList()
        }

        private fun parseTeam(element: JsonElement): GroupDocumentRaw {
            return GroupDocumentRaw(
                id = element.jsonObject["\$id"]!!.jsonPrimitive.content,
                ownerId = "",
                data = element.jsonObject["name"]?.jsonPrimitive?.content ?: "",
                members = emptyList()
            )
        }
    }

}

typealias TeamResponse = String
typealias TeamListResponse = String
typealias TeamMembershipsResponse = String