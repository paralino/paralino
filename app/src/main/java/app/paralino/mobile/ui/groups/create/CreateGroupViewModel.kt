package app.paralino.mobile.ui.groups.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.repo.group.GroupRepository

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