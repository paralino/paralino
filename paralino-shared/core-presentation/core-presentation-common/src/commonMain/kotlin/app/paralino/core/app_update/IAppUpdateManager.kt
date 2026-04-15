package app.paralino.core.app_update

import app.paralino.core.models.base.ConfigData
import app.paralino.core.repo.base.BaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

abstract class IAppUpdateManager: BaseRepository() {

    protected var updateJob: Job? = null
    protected val updateStatus = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    val updateState: StateFlow<AppUpdateStatus> = updateStatus

    abstract fun startUpdateProcess(data: ConfigData)

    fun cancelUpdate() {
        updateJob?.cancel()
        updateStatus.update { AppUpdateStatus.Idle }
    }
}

sealed interface AppUpdateStatus {
    data object Idle: AppUpdateStatus
    data class Downloading(val progress: Float?, val mb: String?): AppUpdateStatus
    data object Installing: AppUpdateStatus
}