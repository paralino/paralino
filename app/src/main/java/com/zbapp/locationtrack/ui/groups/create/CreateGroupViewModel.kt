package com.zbapp.locationtrack.ui.groups.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.groups.CreateGroupViewModel
import com.zbapp.core.repo.group.GroupRepository

class CreateGroupViewModel(
    private val groupId: String?,
    private val groupRepo: GroupRepository
) : ViewModel() {
    private val coreVm = CreateGroupViewModel(groupId, groupRepo, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events
    val groupNameLengthLimit = coreVm.groupNameLengthLimit

    fun createGroup(image: ImageModel, name: String) = coreVm.createGroup(image, name)
    fun updateImage(image: ImageModel) = coreVm.updateImage(image)
}