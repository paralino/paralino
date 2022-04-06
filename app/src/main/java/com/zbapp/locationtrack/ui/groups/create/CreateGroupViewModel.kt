package com.zbapp.locationtrack.ui.groups.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.mapSuccess
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.extensions.mapToPayload
import com.zbapp.locationtrack.ui.groups.detail.GroupDetailsUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val groupId: String?,
    private val groupRepo: GroupRepository
) : ViewModel() {

    private val groupFlow = MutableStateFlow<Payload<UserGroup>>(Payload.Loading)

    val groupDetails = groupFlow
        .map {
            it.mapSuccess { group ->
                GroupDetailsUiModel(
                    id = group.id,
                    name = group.name,
                    image = group.image,
                    memberCount = 0,
                    sharingActive = false,
                    isMyGroup = true
                )
            }
        }
        .asLiveData()

    init {
        viewModelScope.launch {
            groupFlow.value = when {
                groupId.isNullOrBlank() -> null
                else -> groupRepo.getGroupWithoutMembersLocally(groupId)
                    .map { Payload.Success(it) }
                    .firstOrNull()
            } ?: Payload.Error(NullPointerException("No such group"))
        }
    }

    fun createGroup(icon: String, name: String): LiveData<Payload<Unit>> {

        val group = groupFlow.value.data

        return if (group != null) {
            val newData = group.data.copy(name = name, image = ImageModel.emoji(icon))
            val newGroup = group.copy(data = newData)
            groupRepo.updateGroup(newGroup)
        } else {
            groupRepo.createGroup(name, ImageModel.emoji((icon)))
        }
            .mapToPayload<Unit, Unit>()
            .asLiveData()
    }

}