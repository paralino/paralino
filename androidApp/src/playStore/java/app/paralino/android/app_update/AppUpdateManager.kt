package app.paralino.android.app_update

import android.app.Application
import androidx.core.net.toUri
import app.paralino.android.utils.extensions.openUri
import app.paralino.core.auto_update.IAppUpdateManager
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.base.ConfigData
import app.paralino.core.provider.ICoroutineScopeProvider

class AppUpdateManager(
    private val appClass: Application,
    private val scopeProvider: ICoroutineScopeProvider
) : IAppUpdateManager() {
    override fun startUpdateProcess(data: ConfigData) {
        appClass.openUri(BackendResourceIDs.storeUrlGooglePlay.toUri())
    }
}