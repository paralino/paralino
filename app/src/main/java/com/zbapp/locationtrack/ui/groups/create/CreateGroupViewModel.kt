package com.zbapp.locationtrack.ui.groups.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.core.models.Payload
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.mapToPayload

class CreateGroupViewModel(
    private val groupRepo: GroupRepository
): ViewModel() {

    fun createGroup(icon: String, name: String): LiveData<Payload<Unit>> {
        return groupRepo.createGroup(name, icon)
            .mapToPayload<Unit, Unit>()
            .asLiveData()
    }

}