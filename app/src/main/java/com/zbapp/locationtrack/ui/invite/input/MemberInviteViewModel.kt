package com.zbapp.locationtrack.ui.invite.input

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.presentation.invite.input.MemberInviteViewModel
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.invite.InviteRepository

class MemberInviteViewModel(
    app: Application,
    val groupId: String?,
    code: String?,
    private val inviteRepo: InviteRepository,
    private val groupRepo: GroupRepository,
) : AndroidViewModel(app) {

    private val coreVm = MemberInviteViewModel(
        groupId = groupId,
        code = code,
        groupRepo = groupRepo,
        inviteRepo = inviteRepo,
        scope = viewModelScope
    )

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun addMember() = coreVm.addOrLoadMember()
    fun loadMember() = coreVm.addOrLoadMember()
    suspend fun getAllGroups() = coreVm.getAllGroups()
    fun codeChanged(code: String) = coreVm.codeChanged(code)
    fun selectGroup(group: GroupItemData) = coreVm.selectGroup(group)
}

