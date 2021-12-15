package com.zbapp.locationtrack.ui.invite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.zbapp.core.models.Payload
import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.models.mapSuccess
import com.zbapp.core.repo.invite.InviteRepository
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.shared.list.EmptyStateData
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpaceData
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import kotlinx.coroutines.flow.map

class ManageInviteViewModel(
    app: Application,
    private val repo: InviteRepository
) : AndroidViewModel(app) {

    fun deleteInvite(code: InviteCode): LiveData<Payload<Unit>> {
        return repo.revokeInviteCode(code.id).asLiveData()
    }

    fun getAllInvites(): LiveData<Payload<List<ListItemType>>> {
        return repo.getMyInviteCodes()
            .map {
                it.mapSuccess {
                    val codes = it.map { InviteCodeData(it) }
                    if (codes.isEmpty()) {
                        listOf(
                            ListItemEmptySpaceData(100.dp2px),
                            EmptyStateData(
                                iconRes = R.drawable.ic_add_link,
                                infoText = myApp.getString(R.string.invite_manage_empty_description),
                                buttonText = myApp.getString(R.string.invite_create_button)
                            )
                        )
                    } else {
                        mutableListOf<ListItemType>().apply {
                            addAll(codes)
                            add(ListItemEmptySpaceData(20.dp2px))
                            add(EmptyStateData(infoText = myApp.getString(R.string.invite_manage_description)))
                        }
                    }
                }
            }
            .asLiveData()
    }

    fun createInviteCode(userLimit: Int = 1): LiveData<Payload<InviteCode>> {
        return repo.createInviteCode(userLimit).asLiveData()
    }

}