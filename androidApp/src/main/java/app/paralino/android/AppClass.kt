package app.paralino.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.paralino.android.data.PreCheckControl
import app.paralino.android.di.KoinSetup
import app.paralino.android.tracking.LocationService
import app.paralino.android.tracking.getLocationStatus
import app.paralino.core.ParalinoCore
import app.paralino.core.connectionStatus
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.provider.*
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.user.UserRepository
import io.sentry.*
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import kotlin.coroutines.CoroutineContext

class AppClass : Application(), ICoroutineScopeProvider, DefaultLifecycleObserver {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: AppClass
            private set
    }


    private val applicationJob = SupervisorJob()

    override val defaultScope = CoroutineScope(Dispatchers.Default + applicationJob)
    override val defaultIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val userSessionIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val defaultDispatcher: CoroutineContext get() = Dispatchers.Default
    override val ioDispatcher: CoroutineContext get() = Dispatchers.IO

    private val launcher: PreCheckControl by inject()
    private val userRepo: UserRepository by inject()
    private val refreshManager: RefreshEverythingUseCase by inject()
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

        initCrashReporting()
        ParalinoCore.init(
            { androidContext(this@AppClass) },
            KoinSetup.modules
        )

        locationStatus.status.value = LocationConditionStatus.getLocationStatus(this)
        connectionStatus = connectionStatusProvider

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        sessionMonitor.listenForSessionExpired()
            .onEach {
                logg.tag("account_session").v("Got session expired event - logout")
                userSessionIOScope.coroutineContext.cancelChildren()
                logoutUseCase.logoutLocally()
            }
            .launchIn(defaultScope)

        userRepo.localUser
            .onEach {  }
            .launchIn(defaultScope)

        launcher.start()

        locationSharing.sharingStatus
            .onEach {
                logg.v("Location sharing status changed: $it")
                if(it.data == true) {
                    logg.v("Location sharing contract ready, starting location service")
                    // TODO show error to the user if location service can not be started
                    // especially if started from background and user is missing all time location permission
                    LocationService.start(this, isForeground)
                }
            }
            .launchIn(defaultScope)

        registerReceiver(gpsReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply { addAction(Intent.ACTION_PROVIDER_CHANGED) })
        getSystemService<ConnectivityManager>()?.registerDefaultNetworkCallback(object: NetworkCallback() {
            override fun onAvailable(network: Network) { updateConnectionStatus() }
            override fun onLost(network: Network) { updateConnectionStatus() }
        })
    }

    private fun initCrashReporting() {
        SentryAndroid.init(this) {
            it.isSendDefaultPii = false
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        logg.tag("app_state").v("App is in foreground")
        updateLocationStatus()
        updateConnectionStatus()

        if(wasInBackground) {
            wasInBackground = false
            updateConfig()
            updateGroups()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        wasInBackground = true
        logg.tag("app_state").v("App is in background")
    }

    private fun updateLocationStatus() {
        locationStatus.status.value = LocationConditionStatus.getLocationStatus(this)
    }

    private fun updateConnectionStatus() {
        defaultIOScope.launch {
            connectionStatusProvider.initAndGetStatus()
        }
    }

    private fun updateGroups() {
        userSessionIOScope.launch {
            refreshManager.scheduleRefreshAllGroupsWithPlaces(
                time = 60_000
            )
        }
    }

    private fun updateConfig() {
        defaultIOScope.launch {
            try {
                launcher.refreshConfig()
            } catch (e: Exception) {
                logg.e("Error updating config", e)
            }
        }
    }

    val externalLogTree: SystemLogr by lazy {
        object : SystemLogr() {
            override fun systemLog(priority: LogPriority, tag: String?, message: String, t: Throwable?) {
                if(t != null) {
                    Sentry.captureException(t)
                }
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