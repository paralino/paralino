package com.zbapp.locationtrack.ui.invite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.models.mapSuccess
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.invite.InviteRepository
import com.zbapp.core.utils.Emojis
import com.zbapp.core.utils.mapToPayload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.utils.extensions.safeCast
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AcceptInviteViewModel(
    app: Application,
    val args: AccessCodeInputFragmentArgs?,
    private val inviteRepo: InviteRepository,
    private val groupRepo: GroupRepository
) : AndroidViewModel(app) {

    private val inviteCodeData = MutableStateFlow<Payload<AcceptInviteEvent>>(
        Payload.Success(AcceptInviteEvent.InviteEmpty(emptyData()))
    )

    val inviteCodeLiveData = inviteCodeData.asLiveData()

    private val groups = MutableStateFlow<List<UserGroup>>(emptyList())
    private val currentGroup = MutableStateFlow<UserGroup?>(null)
    val currentGroupLiveData = currentGroup.asLiveData()

    init {
        args?.code?.let { loadInvite(code = it) }
        args?.groupId?.let {
            viewModelScope.launch {
                currentGroup.value = groupRepo.getGroupLocally(it).firstOrNull()
            }
        }
    }

    fun acceptOrLoadInvite(code: String?) {
        val currentState = inviteCodeData.value.data as? AcceptInviteEvent.InviteLoaded

        if (currentState is AcceptInviteEvent.InviteLoaded) {
            val groupId = currentGroup.value?.id
            if (groupId.isNullOrBlank()) {
                inviteCodeData.value = Payload.Success(loadedInviteEvent(currentState.invite))
            } else {
                addMemberToGroup(groupId, currentState.invite)
            }
        } else if (!code.isNullOrBlank()) {
            loadInvite(code)
        }
    }

    private fun loadInvite(code: String) {
        inviteRepo.getInviteByCode(code)
            .onEach { inviteCodeData.value = it.mapSuccess { loadedInviteEvent(it) } }
            .launchIn(viewModelScope)
    }

    private fun addMemberToGroup(groupId: String, invite: InviteCode) {
        groupRepo.addMemberByAccessCode(groupId, invite)
            .mapToPayload { it }
            .onEach {
                inviteCodeData.value = it.mapSuccess<GroupDocumentRaw, AcceptInviteEvent> {
                    AcceptInviteEvent.InviteAccepted(inviteCodeData.value.data?.data ?: emptyData())
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadedInviteEvent(invite: InviteCode): AcceptInviteEvent.InviteLoaded {
        return AcceptInviteEvent.InviteLoaded(
            invite,
            InviteUiData(
                code = invite.inviteCode,
                name = invite.ownerName,
                imageEmoji = invite.ownerImageBase64.takeIf { it.isNotBlank() } ?: Emojis.robot,
                description = myApp.getString(
                    R.string.invite_connect_disclaimer_user,
                    invite.ownerName,
                    currentGroup.value?.name ?: ""
                ),
                actionButtonText = myApp.getString(R.string.invite_connect_button_addToGroup),
                groupCount = null
            )
        )
    }

    fun invalidateCode() {
        inviteCodeData.value = Payload.Success(AcceptInviteEvent.InviteEmpty(emptyData()))
    }

    fun setGroup(groupId: String) {
        currentGroup.value = groups.value.firstOrNull { it.id == groupId }
        inviteCodeData.update {
            it.mapSuccess {
                it.safeCast<AcceptInviteEvent.InviteLoaded>()?.let {
                    loadedInviteEvent(it.invite)
                } ?: it
            }
        }
    }

    fun getAllGroups(): LiveData<List<UserGroup>> {
        return groupRepo.getAllGroupsLocally()
            .onEach { groups.value = it }
            .asLiveData()
    }

    fun isGroupValid() = currentGroup.value != null
    fun canPickGroup() =
        args?.groupId.isNullOrBlank() && inviteCodeData.value.data is AcceptInviteEvent.InviteLoaded

    private fun emptyData() = InviteUiData(
        code = "",
        name = null,
        imageEmoji = null,
        description = null,
        actionButtonText = myApp.getString(R.string.button_next),
        groupCount = null
    )

}

sealed interface AcceptInviteEvent {
    abstract val data: InviteUiData

    data class InviteEmpty(override val data: InviteUiData) : AcceptInviteEvent
    data class InviteLoaded(val invite: InviteCode, override val data: InviteUiData) :
        AcceptInviteEvent

    data class InviteAccepted(override val data: InviteUiData) : AcceptInviteEvent
}

data class InviteUiData(
    val code: String,
    val name: String?,
    val imageEmoji: String?,
    val description: String?,
    val actionButtonText: String,
    val groupCount: Int?
)

