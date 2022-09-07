package app.paralino.android.ui.invite.manage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.repo.personal_code.PersonalCodesRepository

class PersonalCodesViewModel constructor(
    app: Application,
    createCode: Boolean,
    private val repo: PersonalCodesRepository,
) : AndroidViewModel(app) {

    private val coreVm = PersonalCodeViewModel(repo, createCode, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun createPersonalCode() = coreVm.createPersonalCode()
    fun deletePersonalCode(codeId: String) = coreVm.deletePersonalCode(codeId)

}