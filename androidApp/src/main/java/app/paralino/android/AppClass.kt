package app.paralino.android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.os.Build
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.paralino.android.data.PreCheckControl
import app.paralino.android.di.KoinSetup
import app.paralino.android.notification.NotificationSocketServiceLauncher
import app.paralino.android.tracking.LocationService
import app.paralino.android.tracking.LocationServiceLauncher
import app.paralino.core.CoreAppManager
import app.paralino.core.ParalinoCore
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.CoreAppClass
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import kotlin.coroutines.CoroutineContext

class AppClass : CoreAppClass(), ICoroutineScopeProvider, DefaultLifecycleObserver {

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

    private val androidPreCheck: PreCheckControl by inject()
    private val coreAppManager: CoreAppManager by inject()
    private val locationSharingManager: LocationSharingManager by inject()
    private val deviceApiProvider: IDeviceApiProvider by inject()
    private val locationServiceLauncher: LocationServiceLauncher by inject()
    private val notificationSocketServiceLauncher: NotificationSocketServiceLauncher by inject()
    private val appPrefs: AppPrefs by inject()

    val appState get() = ProcessLifecycleOwner.get().lifecycle.currentState

    override fun onCreate() {
        super<CoreAppClass>.onCreate()
        init()
    }

    private fun init() {
        instance = this
        CoreAppClass.instance = this

        initCrashReporting()
        ParalinoCore.init(
            { androidContext(this@AppClass) },
            KoinSetup.modules
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        androidPreCheck.start()
        coreAppManager.onAppStart()
        initBackgroundServices()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                gpsReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply { addAction(Intent.ACTION_PROVIDER_CHANGED) },
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                gpsReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply { addAction(Intent.ACTION_PROVIDER_CHANGED) }
            )
        }
        getSystemService<ConnectivityManager>().let {
            if (it != null) {
                logg.i("app_state", "Register system connectivity monitor")
                it.registerDefaultNetworkCallback(
                    object : NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            coreAppManager.updateConnectionStatus()
                        }

                        override fun onLost(network: Network) {
                            coreAppManager.updateConnectionStatus()
                        }
                    }
                )
            } else {
                logg.i("app_state", "System connectivity monitor not available")
            }
        }
    }

    private fun initCrashReporting() {
        SentryAndroid.init(this) {
            it.isSendDefaultPii = false
            it.isEnableUserInteractionTracing = false
            it.isAttachScreenshot = false
            it.isAttachViewHierarchy = false
            it.isEnabled = AppPrefs.instance.errorReporting
        }
    }

    private fun initBackgroundServices() {
        combine(
            locationSharingManager.sharingStatus,
            deviceApiProvider.appLifecycleState
        ) { sharingStatus, appState ->
            sharingStatus to appState
        }
            .debounce(500)
            .onEach {
                if (it.second.current == AppLifecycleType.ACTIVE) {
                    locationServiceLauncher.startLocationService(
                        action = LocationService.INTENT_ACTION_LOCATION_SERVICE_START
                    )
                }
            }
            .launchIn(defaultScope)

        notificationSocketServiceLauncher.startMonitoring(defaultScope)
    }

    override fun onStart(owner: LifecycleOwner) {
        coreAppManager.onAppActive()
    }

    override fun onResume(owner: LifecycleOwner) {
        coreAppManager.onAppActive()
        Sentry.configureScope {
            it.user = (it.user ?: User()).apply { id = appPrefs.errorReportingID }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        coreAppManager.onAppInactive()
    }

    override fun onStop(owner: LifecycleOwner) {
        coreAppManager.onAppBackground()
    }

    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                coreAppManager.updateLocationStatus()
                locationServiceLauncher.startLocationService(action = intent.action)
            }
        }
    }

    override fun onTerminate() {
        applicationJob.cancel()
        super.onTerminate()
    }
}