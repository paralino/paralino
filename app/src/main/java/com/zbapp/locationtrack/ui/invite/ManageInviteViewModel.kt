package com.zbapp.locationtrack.ui.invite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.presentation.invite.PersonalCodeViewModel
import com.zbapp.core.repo.invite.InviteRepository

class ManageInviteViewModel(
    app: Application,
    private val repo: InviteRepository,
) : AndroidViewModel(app) {

    private val coreVm = PersonalCodeViewModel(repo, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun createInviteCode() = coreVm.createInviteCode()
    fun deleteInvite(codeId: String) = coreVm.deleteInvite(codeId)

}