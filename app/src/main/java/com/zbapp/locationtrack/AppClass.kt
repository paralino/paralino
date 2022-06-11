package com.zbapp.locationtrack

import Logger
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.config
import com.zbapp.core.connectionStatus
import com.zbapp.core.domain.user.LogoutUseCase
import com.zbapp.core.domain.user.UserSessionMonitor
import com.zbapp.core.logg
import com.zbapp.core.models.data
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.IConnectionStatusProvider
import com.zbapp.core.utils.ILocationStatusProvider
import com.zbapp.core.utils.LocationConditionStatus
import com.zbapp.locationtrack.data.PreCheckControl
import com.zbapp.locationtrack.data.config.Config
import com.zbapp.locationtrack.di.KoinSetup
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.tracking.LocationService
import com.zbapp.locationtrack.tracking.getLocationStatus
import com.zbapp.locationtrack.utils.logger.DebugLogger
import com.zbapp.locationtrack.utils.logger.LoggerKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import logv
import org.koin.android.ext.android.inject

class AppClass : Application(), ICoroutineScopeProvider, DefaultLifecycleObserver {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: AppClass
            private set
    }


    private val applicationJob = SupervisorJob()

    override val defaultScope = CoroutineScope(Dispatchers.Default + applicationJob)
    override val defaultIOScope = CoroutineScope(Dispatchers.IO)

    private val launcher: PreCheckControl by inject()
    private val userRepo: UserRepository by inject()
    private val sessionMonitor: UserSessionMonitor by inject()
    private val logoutUseCase: LogoutUseCase by inject()
    private val connectionStatusProvider: IConnectionStatusProvider by inject()

    private val locationInit: LocationInitHandler by inject()
    private val locationSharing: LocationSharingManager by inject()
    val locationStatus: ILocationStatusProvider by inject()

    val isForeground get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    override fun onCreate() {
        super<Application>.onCreate()
        init()
    }

    private fun init() {
        instance = this
        context = applicationContext

        KoinSetup.initKoin(this)

        locationStatus.status.value = LocationConditionStatus.getLocationStatus(this)
        connectionStatus = connectionStatusProvider
        config = Config(context)
        appConfig = config as Config

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        initLogger()

        sessionMonitor.listenForSessionExpired()
            .onEach {
                logoutUseCase.logoutLocally()
            }
            .launchIn(defaultScope)

        userRepo.localUser
            .onEach {  }
            .launchIn(defaultScope)

        launcher.start()
            .catch { logg.d("Error with initial refresh: ${it.message}") }
            .launchIn(defaultScope)

        locationSharing.sharingStatus
            .onEach {
                logv("Location sharing status changed: $it")
                if(it.data == true) {
                    logv("Location sharing contract ready, starting location service")
                    LocationService.start(this, isForeground)
                }
            }
            .launchIn(defaultScope)

        registerReceiver(gpsReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply { addAction(Intent.ACTION_PROVIDER_CHANGED) })
    }

    private fun initLogger() {
        logg = Logger
        if (!BuildConfig.forceLogToSystem) {
            Logger.setLogger(externalLogTree)
        } else {
            Logger.setLogger(DebugLogger(getString(R.string.app_name)))
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        updateLocationStatus()
    }

    fun updateLocationStatus() {
        locationStatus.status.value = LocationConditionStatus.getLocationStatus(this)
    }

    private val externalLogTree: LoggerKind by lazy {
        object : LoggerKind() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // TODO maybe log to file
            }
        }
    }

    private val gpsReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                updateLocationStatus()
            }
        }
    }

    override fun onTerminate() {
        applicationJob.cancel()
        super.onTerminate()
    }

}

lateinit var appConfig: Config