package com.zbapp.locationtrack.ui.invite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.mapSuccess
import com.zbapp.core.repo.invite.InviteRepository
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AcceptInviteViewModel(
    app: Application,
    private val repo: InviteRepository
) : AndroidViewModel(app) {

    private val code = MutableStateFlow<String?>("")
    private val inviteCodeData = MutableStateFlow<Payload<AcceptInviteEvent>>(
        Payload.Success(AcceptInviteEvent.InviteAccept(emptyData("")))
    )

    val inviteCodeLiveData = inviteCodeData.asLiveData()

    fun invalidateCode() {
        this.code.value = null
        inviteCodeData.value = Payload.Success(AcceptInviteEvent.InviteAccept(emptyData("")))
    }

    fun acceptOrLoadInvite(code: String?) {
        this.code.value = code
        val currentState = inviteCodeData.value.data
        if (currentState is AcceptInviteEvent.InviteLoaded) {
            repo.acceptInvite(currentState.data.code)
                .onEach {
                    inviteCodeData.value = it.mapSuccess {
                        AcceptInviteEvent.InviteAccept(emptyData(currentState.data.code))
                    }
                }
                .launchIn(viewModelScope)
        } else if (!code.isNullOrBlank()) {
            repo.getInviteByCode(code)
                .onEach {
                    inviteCodeData.value = it.mapSuccess { invite ->
                        AcceptInviteEvent.InviteLoaded(
                            InviteUiData(
                                code = invite.inviteCode,
                                name = if (invite.isGroupInvite) invite.groupName
                                    ?: "" else invite.ownerName,
                                description = if (invite.isGroupInvite) myApp.getString(
                                    R.string.invite_connect_disclaimer_group,
                                    invite.groupName ?: ""
                                )
                                else null,
                                imageEmoji = if (invite.isGroupInvite) invite.groupEmoji
                                    ?: Emojis.lock else invite.ownerImageBase64.takeIf { it.isNotBlank() }
                                    ?: Emojis.robot,
                                actionButtonText = if (invite.isGroupInvite) myApp.getString(R.string.invite_connect_button_request)
                                else myApp.getString(R.string.invite_connect_button_addToGroup),
                                groupCount = if (invite.isGroupInvite) invite.groupMembersCount else null
                            )
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun emptyData(code: String) = InviteUiData(
        code = code,
        name = null,
        description = null,
        imageEmoji = null,
        actionButtonText = myApp.getString(R.string.button_confirm),
        groupCount = null
    )

}

sealed interface AcceptInviteEvent {
    abstract val data: InviteUiData

    data class InviteLoaded(override val data: InviteUiData) : AcceptInviteEvent
    data class InviteAccept(override val data: InviteUiData) : AcceptInviteEvent
}

data class InviteUiData(
    val code: String,
    val name: String?,
    val description: String?,
    val imageEmoji: String?,
    val actionButtonText: String,
    val groupCount: Int?
)

