package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class GroupRepository {

    private val groups = MutableStateFlow(emptyList<UserGroup>())
    private val personalTrackingGroup get() = UserGroup.personalTracking
    private val publicSharingGroup get() = UserGroup.publicSharing

    fun getAllGroups(): Flow<List<UserGroup>> {
        return groups.map {
            it + listOfNotNull(personalTrackingGroup, publicSharingGroup)
        }
    }

    fun getMyGroups(): Flow<List<UserGroup>> {
        return groups
    }

    fun getGroup(id: String): Flow<UserGroup?> {
        return groups.map { it.firstOrNull { it.id == id } }
    }

    fun createOrUpdateGroup(userGroup: UserGroup) {
        groups.value = groups.value.toMutableList().apply {
            removeAll { it.id == userGroup.id }
            add(userGroup)
        }
    }

    fun deleteGroup(id: String) {
        groups.value = groups.value.toMutableList().apply {
            removeAll { it.id == id }
        }
    }

}