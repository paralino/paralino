package com.zbapp.locationtrack.ui.invite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import com.zbapp.core.models.Payload
import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.repo.invite.InviteRepository
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.mapToPayload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.shared.list.EmptyStateData
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpaceData
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px

class ManageInviteViewModel(
    app: Application,
    private val repo: InviteRepository,
    private val userRepo: UserRepository
) : AndroidViewModel(app) {

    fun deleteInvite(code: InviteCode): LiveData<Payload<Unit>> {
        return repo.revokeInviteCode(code.id)
            .mapToPayload { it }
            .asLiveData()
    }

    fun getAllInvites(): LiveData<Payload<List<ListItemType>>> {
        return repo.getMyInviteCodes()
            .mapToPayload {
                it.map { InviteCodeData(it) }.ifEmpty {
                    listOf(
                        ListItemEmptySpaceData(100.dp2px),
                        EmptyStateData(
                            iconRes = R.drawable.ic_add_link,
                            infoText = myApp.getString(R.string.invite_manage_empty_description),
                            buttonText = myApp.getString(R.string.invite_create_button)
                        )
                    )
                }
            }
            .asLiveData()
    }

    fun createInviteCode(userLimit: Int = 1): LiveData<Payload<InviteCode>> {
        val user = userRepo.cachedUser
            ?: return liveData { emit(Payload.Error(Throwable("User not valid"))) }
        val invite = InviteCode.appInvite(
            ownerId = user.id,
            ownerName = user.name,
            ownerImageBase64 = user.data?.imageBase64 ?: "",
            userLimit = userLimit
        )

        return repo.createInviteCode(invite)
            .mapToPayload { it }
            .asLiveData()
    }

}