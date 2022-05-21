package com.zbapp.core.presentation.share

import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.utils.ConditionErrorType

data class SharingStartScreenState(
    val groupState: GroupItemData? = null,
    val errors: List<ConditionErrorType> = emptyList(),
    val loadingAction: Boolean = false
)

sealed class SharingStartScreenEvent() {
    data class Error(val error: Throwable): SharingStartScreenEvent()
    object Started: SharingStartScreenEvent()
    object SelectGroupInfo: SharingStartScreenEvent()
    object LocationStatusInsufficient: SharingStartScreenEvent()
    object MissingBackgroundPermission: SharingStartScreenEvent()
}
