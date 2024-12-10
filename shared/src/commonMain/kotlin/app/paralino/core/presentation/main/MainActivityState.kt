package app.paralino.core.presentation.main

import app.paralino.core.app_update.AppUpdateStatus
import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.models.Resource
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.ui.main.DeeplinkType

data class MainActivityState(
    val userState: Resource<AppAccountState> = Resource.NotInitialized,
    val updateType: AppUpdateType? = null,
    val isSessionExpiredShown: Boolean = false,
    val appUpdateStatus: AppUpdateStatus = AppUpdateStatus.Idle,
    val dataRefreshBlockingLoadingShown: Boolean = false,
    val pendingDeeplinkType: DeeplinkType? = null,
    val didHaveWelcomeOnboarding: Boolean = false
) {
    companion object {
        // used in iOS since KMM still does not propagates default values
        fun newInstance(): MainActivityState {
            return MainActivityState()
        }
    }
}
