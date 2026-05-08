package app.paralino.core.presentation.main

import app.paralino.core.app_update.AppUpdateStatus
import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.models.Resource
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.utils.DeeplinkType

data class MainActivityState(
    val userState: Resource<AppAccountState> = Resource.NotInitialized,
    val appUpdateStatus: AppUpdateStatus = AppUpdateStatus.Idle,
    val dataRefreshBlockingLoadingShown: Boolean = false,
    val pendingDeeplinkType: DeeplinkType? = null,
    val updateType: AppUpdateType? = null,
    val sessionExpiredDialog: MainAlertDialog? = null
) {

    val appUpdateDialog: MainAlertDialog? = when (updateType) {
        null,
        is AppUpdateType.NONE,
        is AppUpdateType.PASSIVE -> null

        is AppUpdateType.SOFT,
        is AppUpdateType.SOFT_CONSTANT, -> MainAlertDialog.AppVersionInfo(forceUpdate = false)

        is AppUpdateType.FORCE,
        is AppUpdateType.FORCE_AND_CANCEL -> MainAlertDialog.AppVersionInfo(forceUpdate = true)
    }

    companion object {
        // used in iOS since KMM still does not propagates default values
        fun newInstance(): MainActivityState {
            return MainActivityState()
        }
    }
}


sealed interface MainActivityUiEvent {
    data class DialogAction(val action: AlertDialogAction) : MainActivityUiEvent
}