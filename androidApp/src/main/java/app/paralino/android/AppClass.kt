package app.paralino.android

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
import app.paralino.android.data.PreCheckControl
import app.paralino.android.data.config.Config
import app.paralino.android.di.KoinSetup
import app.paralino.android.tracking.LocationService
import app.paralino.android.tracking.getLocationStatus
import app.paralino.android.utils.logger.DebugLogger
import app.paralino.android.utils.logger.LoggerKind
import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.config
import app.paralino.core.connectionStatus
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.IConnectionStatusProvider
import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.LocationConditionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    override val userSessionIOScope = CoroutineScope(Dispatchers.IO)

    private val launcher: PreCheckControl by inject()
    private val userRepo: UserRepository by inject()
    private val groupRepo: GroupRepository by inject()
    private val sessionMonitor: UserSessionMonitor by inject()
    private val logoutUseCase: LogoutUseCase by inject()
    private val connectionStatusProvider: IConnectionStatusProvider by inject()

    private val locationSharing: LocationSharingManager by inject()
    val locationStatus: ILocationStatusProvider by inject()

    val isForeground get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    private var wasInBackground = false

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

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        initLogger()

        sessionMonitor.listenForSessionExpired()
            .onEach {
                userSessionIOScope.coroutineContext.cancelChildren()
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
        logg.tag("app_state").v("App is in foreground")
        updateLocationStatus()

        if(wasInBackground) {
            wasInBackground = false
            userSessionIOScope.launch {
                groupRepo.scheduleRefreshAllGroupsWithPlaces(
                    time = 60_000
                )
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        wasInBackground = true
        logg.tag("app_state").v("App is in background")
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