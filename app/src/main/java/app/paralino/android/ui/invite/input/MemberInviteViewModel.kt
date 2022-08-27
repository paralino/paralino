package app.paralino.android.ui.invite.input

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.presentation.dashboard.GroupItemData
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository

class MemberInviteViewModel(
    app: Application,
    val groupId: String?,
    code: String?,
    private val codeRepo: PersonalCodesRepository,
    private val groupRepo: GroupRepository,
) : AndroidViewModel(app) {

    private val coreVm = MemberInviteViewModel(
        groupId = groupId,
        code = code,
        groupRepo = groupRepo,
        codeRepo = codeRepo,
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

